package controllers

import java.sql.Connection

import play.api._
import play.api.data.validation.{Valid, Invalid, Constraint}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Logger
import play.api.i18n.Lang
import anorm._
import play.api.db.DB
import play.api.Play.current
import anorm.SqlParser._


object Application extends Controller {
  case class Ballot(preferences: List[Int])
  case class DisplayOrder(candidateIds: List[Int])
  case class Election(candidates: List[String], name: String, description: String)

  def voteForm(candidates: List[String]) = {
    def validateRankingIsComplete(preferences: List[Int]): Boolean = {
      val received = preferences.sorted.toList
      val expected = 1.to(candidates.length).toList
      Logger.debug(received.toString)
      Logger.debug(expected.toString)
      received == expected
    }
    Form(
      mapping(
        "preferences" -> list(number)
      )(Ballot.apply)(Ballot.unapply) verifying(
        "Ranking is not complete",
        fields => validateRankingIsComplete(fields.preferences)
        )
    )
  }

  val voteDisplayOrderForm: Form[DisplayOrder] = Form(
    mapping(
      "displayOrder" -> list(number)
    )(DisplayOrder.apply)(DisplayOrder.unapply)
  )

  val electionForm: Form[Election] = {
    val filterNonEmpty: List[String] => List[String] = _.filter(_ != "")
    Form(
      mapping(
        "candidates" -> list(text).transform(filterNonEmpty, identity[List[String]]).verifying(
          "Two or more items required", _.size >= 2
        ),
        "name" -> nonEmptyText,
        "description" -> text
      )(Election.apply)(Election.unapply)
    )
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def show(election: Long) = Action { implicit request =>
    val rows: List[Row] = getCandidates(election)
    if(rows.isEmpty) {
      NotFound
    } else {
      val first = rows.head
      val name = first[String]("name")
      val desc = first[String]("description")
      val candidates = rows map (_("candidate"))
      Ok(views.html.show(name, desc, candidates))
    }

  }

  def getCandidates(election: Long): List[Row] = {
    val sql = SQL"""
      select e.name, e.description, c.name as candidate, c.id as candidateId
      from Election e INNER JOIN Candidate c ON (e.Id = c.electionId)
      where e.id = $election
      order by c.id
    """
    DB.withConnection { implicit c => sql().toList}
  }

  def showVoteForm(election: Long) = Action {
    val rows = getCandidates(election)
    val candidates = rows.map(_[String]("candidate"))
    val ids: List[Int] = rows.map(_[Int]("candidateId"))
    Ok(views.html.voteForm(election, rows.head[String]("name"), candidates, voteForm(candidates)))
  }

  def vote(election: Long) = Action { implicit request =>
    val rows = getCandidates(election)
    val candidates = rows.map(_[String]("candidate"))
    val ids = rows.map(_[Int]("candidateId"))
    def insertBallot(name: String)(implicit c: Connection): Option[Long] = {
      SQL"insert into Ballot(name) values($name)".executeInsert()
    }

    def insertPreferences(ranking: Seq[(Int, Int)], ballotId: Long)(implicit c: Connection): Array[Int] = {
        BatchSql(
          SQL("insert into Preference(candidateId, ballotId, rank) values ({candidateId}, {ballotId}, {rank})"),
          for ((candidateId, rank) <- ranking)
          yield Seq[NamedParameter]("candidateId" -> candidateId, "rank" -> rank, "ballotId" -> ballotId)
        ).execute() // Throws SQLExceptions
    }

    voteForm(candidates).bindFromRequest.fold(
      formWithErrors => {
        Logger.debug("Oh no")
        Logger.debug(formWithErrors.errorsAsJson(Lang("en")).toString)
        val prefilled = request.body.asFormUrlEncoded
        BadRequest(views.html.voteForm(election, "Test Vote", candidates, formWithErrors))
      },
      value => {
        Logger.debug("Yay")
        DB.withConnection { implicit c =>
          val ballotId = insertBallot("foo").get
          insertPreferences(ids.zip(value.preferences), ballotId)
        }
        Redirect(routes.Application.show(election)).flashing("success" -> "Your vote has been recorded.")
      }
    )
  }

  def count(election: Long) = TODO

  def showNewElectionForm() = Action {
    Ok(views.html.electionForm(electionForm))
  }

  def createElection() = Action {implicit request =>

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

    electionForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.debug("Oh no")
        Logger.debug(formWithErrors.errorsAsJson(Lang("en")).toString)
        BadRequest(views.html.electionForm(formWithErrors))
      },
      value => {
        Logger.debug("Yay")
        val id = DB.withConnection {implicit c => insertElectionAndCandidates(value)}
        Redirect(routes.Application.show(id)).flashing("success" -> "Created new choice.")
      }
    )}

}