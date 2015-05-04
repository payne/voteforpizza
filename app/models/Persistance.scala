package models

import java.sql.Connection
import anorm._
import anorm.SqlParser.scalar

/**
 * Methods to interact with the Postgres DB
 */
object Persistance {

  def selectCandidates(election: Long)(implicit c: Connection): List[Row] = {
    val sql = SQL"""
      select e.name, e.description, c.name as candidate, c.id as candidateId
      from Election e INNER JOIN Candidate c ON (e.Id = c.electionId)
      where e.id = $election
      order by c.id
    """
    sql().toList
  }

  def selectVoters(election: Long)(implicit c: Connection): List[String] = {
    val sql = SQL"""
      select name
      from ballot
      where electionId = $election
    """
    sql.as(scalar[String].*)
  }

  def selectBallots(election: Long)(implicit c: Connection): Map[Long, List[Row]] = {
    val sql = SQL"""
      select
        p.ballotId,
        p.candidateId,
        p.rank
      from ballot b inner join preference p on (p.ballotId = b.id)
      where b.electionId = $election
      order by p.ballotId, p.rank
    """
    sql().toList.groupBy(_[Long]("ballotId"))
  }

  def selectCounts(election: Long)(implicit c: Connection): List[List[Row]] = {
    val sql = SQL"""
      select
        d.candidateId,
        ca.name as candidateName,
        d.countId,
        d.round,
        d.isElected,
        d.voteCount
      from ElectionCount c
      inner join ElectionCountDetail d on (d.countId = c.id)
      inner join Candidate ca on (d.candidateId = ca.id)
      where c.electionId = $election
      order by d.countId, d.round, ca.name
    """
    // This sucks :(
    def groupByColumn(rows: List[Row], column: String): List[List[Row]] = {
      if(rows.isEmpty) {
        List()
      } else {
        val firstGroup = rows.head[Long](column)
        val (firstGroupRows, otherGroupRows) = rows.span(_[Long](column) == firstGroup)
        if(otherGroupRows.isEmpty) {
          List(firstGroupRows)
        } else {
          firstGroupRows :: groupByColumn(otherGroupRows, column)
        }
      }
    }
    val allRows = sql().toList
    val countRows = groupByColumn(allRows, "countId")
    //countRows map {groupByColumn(_, "round")}
    countRows
  }

  // TODO create a model object that maps election results to candidate IDs
  // so we don't have to deal with it here
  def insertCount(electionId: Long, seats: Int, rounds: List[stv.ElectionResult], candidateMap: Map[stv.Candidate, Long])(implicit c: Connection) = {
    def insertCount: Option[Long] = {
      SQL"insert into ElectionCount(electionId, seats) values($electionId, $seats)".executeInsert()
    }
    def insertDetail(countId: Long) = {
      BatchSql(
        SQL("insert into ElectionCountDetail(countId, round, candidateId, isElected, voteCount) values({countId}, {round}, {candidateId}, {isElected}, {voteCount})"),
        for (
          roundDetail <- rounds.zipWithIndex;
          candidateResult <- (roundDetail._1.elected ++ roundDetail._1.hopefuls ++ roundDetail._1.eliminated);
          candidateId = (candidateResult match {
            case stv.ElectedCandidate(candidate, _) => candidateMap(candidate)
            case stv.HopefulCandidate(candidate, _) => candidateMap(candidate)
            case stv.EliminatedCandidate(candidate) => candidateMap(candidate)
          });
          voteCount = (candidateResult match {
            case stv.ElectedCandidate(_, votes) => votes.length
            case stv.HopefulCandidate(_, votes) => votes.length
            case stv.EliminatedCandidate(_) => 0
          })
        ) yield Seq[NamedParameter](
          "round" -> (roundDetail._2 + 1),
          "countId" -> countId,
          "isElected" -> (candidateResult match {case stv.ElectedCandidate(_, _) => true; case _ => false}),
          "voteCount" -> voteCount,
          "candidateId" -> candidateId
        )
      ).execute()
    }
    insertDetail(insertCount.get)
  }

  def insertBallot(name: String, electionId: Long)(implicit c: Connection): Option[Long] = {
      SQL"insert into Ballot(name, electionId) values($name, $electionId)".executeInsert()
  }

  def insertPreferences(ranking: Seq[(Int, Int)], ballotId: Long)(implicit c: Connection): Array[Int] = {
    BatchSql(
      SQL("insert into Preference(candidateId, ballotId, rank) values ({candidateId}, {ballotId}, {rank})"),
      for ((candidateId, rank) <- ranking)
      yield Seq[NamedParameter]("candidateId" -> candidateId, "rank" -> rank, "ballotId" -> ballotId)
    ).execute() // Throws SQLExceptions
  }

  def insertElection(name: String, description: String)(implicit c: Connection): Option[Long] = {
    SQL"insert into Election(name, description) values($name, $description)".executeInsert()
  }

  def insertCandidates(candidates: List[String], electionId: Long)(implicit c: Connection): Array[Int] = {
    BatchSql(
      SQL("insert into Candidate(name, electionId) values ({name}, {eId})"),
      for (candidate <- candidates)
      yield Seq[NamedParameter]("name" -> candidate, "eId" -> electionId)
    ).execute() // Throws SQLExceptions
  }

  def insertElectionAndCandidates(election: Election)(implicit c: Connection): Long = {
    val insertedId: Option[Long] = insertElection(election.name, election.description)

    insertedId match {
      case Some(id) => {
        insertCandidates(election.candidates, id)
        id
      }
      case None => {
        throw new Exception("Fuck it")
      }
    }
  }

}
