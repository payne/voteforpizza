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


object Application extends Controller {
  val candidates = List("foo", "bar", "baz")
  case class Ballot(preferences: List[Int])
  case class Election(candidates: List[String], name: String, description: String)

  def validateRankingIsComplete(preferences: List[Int]): Boolean = {
    val received = preferences.sorted.toList
    val expected = 1.to(candidates.length).toList
    Logger.debug(received.toString)
    Logger.debug(expected.toString)
    received == expected
  }

  val voteForm: Form[Ballot] = Form(
    mapping(
      "preferences" -> list(number)
    )(Ballot.apply)(Ballot.unapply) verifying(
      "Ranking is not complete",
      fields => validateRankingIsComplete(fields.preferences)
    )
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

  def show(election: String) = Action { implicit request =>
    Ok(views.html.show("Test Vote", "lorum ipsum", List("apples", "oranges")))
  }

  def showVoteForm(election: String) = Action {
    Ok(views.html.voteForm(election, "Test Vote", candidates, voteForm))
  }

  def vote(election: String) = Action { implicit request =>
    voteForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.debug("Oh no")
        Logger.debug(formWithErrors.errorsAsJson(Lang("en")).toString)
        val prefilled = request.body.asFormUrlEncoded
        BadRequest(views.html.voteForm(election, "Test Vote", candidates, formWithErrors))
      },
      value => {
        Logger.debug("Yay")
        Redirect(routes.Application.show(election)).flashing("success" -> "Your vote has been recorded.")
      }
    )
  }

  def count(election: String) = TODO

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
        Redirect(routes.Application.show(id.toString)).flashing("success" -> "Created new choice.")
      }
    )}

}