package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._


object Application extends Controller {
  val candidates = List("foo", "bar", "baz")
  case class Ballot(preferences: List[Int])

  def validateRankingIsComplete(preferences: List[Int]) = {
    preferences.sorted == 1.until(candidates.length)
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

  def show(election: String) = Action {
    Ok(views.html.show("Test Vote", "lorum ipsum", List("apples", "oranges")))
  }

  def showVoteForm(election: String) = Action {
    Ok(views.html.voteForm(election, "Test Vote", candidates, voteForm))
  }

  def vote(election: String) = TODO

  def count(election: String) = TODO

}