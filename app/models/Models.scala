package models

/**
 * Data models used by forms.
 */
case class Election(candidates: List[String], name: String, description: String)
case class Ballot(preferences: List[Int])
