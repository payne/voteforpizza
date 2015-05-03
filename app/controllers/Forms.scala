package controllers
import models.Election
import models.Ballot
import play.api.data._
import play.api.data.Forms._
import play.api.Logger

/**
 * Form definitions
 */
object Forms {
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
}
