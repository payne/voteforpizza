package controllers

import play.api.mvc._
import play.api.Logger
import play.api.i18n.Lang
import anorm._
import models.Persistance._
import play.api.db.DB
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    Redirect(routes.Application.showNewElectionForm)
  }

  def show(election: Long) = Action { implicit request =>
    val rows: List[Row] = DB.withConnection {implicit c => selectCandidates(election)}
    if(rows.isEmpty) {
      NotFound
    } else {
      val first = rows.head
      val name = first[String]("name")
      val desc = first[String]("description")
      val candidates = rows map (_("candidate"))
      val voters: List[String] = DB.withConnection {implicit c => selectVoters(election)}
      val counts: List[List[Row]] = DB.withConnection {implicit c => selectCounts(election)}
      Ok(views.html.show(election, name, desc, candidates, voters, counts, Forms.countForm))
    }

  }

  def showVoteForm(election: Long) = Action {
    val rows: List[Row] = DB.withConnection {implicit c => selectCandidates(election)}
    val candidates = rows.map(_[String]("candidate"))
    val ids: List[Int] = rows.map(_[Int]("candidateId"))
    Ok(views.html.voteForm(election, rows.head[String]("name"), candidates, Forms.voteForm(candidates)))
  }

  def vote(election: Long) = Action { implicit request =>
    val rows: List[Row] = DB.withConnection {implicit c => selectCandidates(election)}
    val candidates = rows.map(_[String]("candidate"))
    val ids = rows.map(_[Int]("candidateId"))

    Forms.voteForm(candidates).bindFromRequest.fold(
      formWithErrors => {
        Logger.debug("Oh no")
        Logger.debug(formWithErrors.errorsAsJson(Lang("en")).toString)
        val prefilled = request.body.asFormUrlEncoded
        BadRequest(views.html.voteForm(election, "Test Vote", candidates, formWithErrors))
      },
      value => {
        Logger.debug("Yay")
        DB.withConnection { implicit c =>
          val ballotId = insertBallot(value.name, election).get
          insertPreferences(ids.zip(value.preferences), ballotId)
        }
        Redirect(routes.Application.show(election)).flashing("success" -> "Your vote has been recorded.")
      }
    )
  }

  def count(election: Long) = Action { implicit request =>
    Forms.countForm.bindFromRequest.fold(
      formWithErrors => {
        Logger.debug("Oh no")
        Logger.debug(formWithErrors.errorsAsJson(Lang("en")).toString)
        Redirect(routes.Application.show(election)).flashing("error" -> "Invalid number of seats.")
      },
      seats => {
        Logger.info(seats.toString)
        val rows: List[Row] = DB.withConnection {implicit c => selectCandidates(election)}
        val candidateIdMap = (for(row <- rows) yield (row[Long]("candidateId"), stv.Candidate(row[String]("candidate")))).toMap
        val ballots = DB.withConnection {implicit c=> selectBallots(election)}
        val electionObj = stv.Election(candidateIdMap.values.toSet, seats=seats)
        Logger.info(electionObj.seats.toString)
        def rankingFromRows(rows: List[Row]) = {
          (for(row <- rows) yield candidateIdMap(row[Long]("candidateId"))).toList
        }
        if(ballots.isEmpty) {
          Redirect(routes.Application.show(election)).flashing("error" -> "Nothing to count.")
        } else {
          val electionWithVotes = ballots.values.foldLeft(electionObj) { (el: stv.Election, rows: List[Row]) =>
            el.vote(rankingFromRows(rows))
          }
          Logger.info(electionWithVotes.seats.toString)
          val count = stv.SomeCount(electionWithVotes)
          for (round <- count.roundCounts) {
            Logger.info(round.toString)
          }

          DB.withConnection { implicit c => insertCount(election, electionWithVotes.seats, count.roundCounts, candidateIdMap.map(_.swap))}
          Redirect(routes.Application.show(election)).flashing("success" -> "Election has been counted.")
        }
      }
    )
  }

  def showNewElectionForm() = Action {
    Ok(views.html.electionForm(Forms.electionForm))
  }

  def createElection() = Action {implicit request =>
    Forms.electionForm.bindFromRequest.fold(
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