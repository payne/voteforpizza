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
