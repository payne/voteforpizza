package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def show(election: String) = TODO

  def voteForm(election: String) = TODO

  def vote(election: String) = TODO

  def count(election: String) = TODO

}