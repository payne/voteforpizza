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
      received.length > 0 && received.zip(expected).forall(pair => pair._1 == pair._2)
    }
    Form(
      mapping(
        "preferences" -> list(optional(number)),
        "name" -> text(maxLength=100)
      )(Ballot.apply)(Ballot.unapply) verifying(
        "Ranking is not complete",
        fields => validateRankingIsComplete(fields.preferences.collect{case Some(x) => x})
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
        "name" -> nonEmptyText(maxLength=200),
        "description" -> text(maxLength=1000)
      )(Election.apply)(Election.unapply)
    )
  }

  val countForm: Form[Int] = Form(
    mapping(
      "seats" -> number(min=1)
    )(identity[Int])(Some[Int](_)))
}
