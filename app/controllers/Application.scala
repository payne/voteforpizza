package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Logger
import play.api.i18n.Lang


object Application extends Controller {
  val candidates = List("foo", "bar", "baz")
  case class Ballot(preferences: List[Int])

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

}