package ru.emkn.kotlin.sms.classes

import java.time.Duration

data class Team(val teamName: String, val athletes: List<Athlete>)

data class CompetitorsGroup(val group: Group, val competitors: List<Competitor>)

data class CompetitorsDataGroup(val group: Group, val competitorsData: List<CompetitorData>)

data class TeamResults(val teamName: String, val teamScore: Int, val data: List<CompetitorResultInTeam>)

data class GroupResults(val group: Group, val results: List<CompetitorResultInGroup>) {

    private val leaderTime: Duration?
        get() = this.results[0].result

    fun getCompetitorScore(athleteNumber: Int): Int {
        val athleteTime = this.results.find { resultAthleteGroup ->
            resultAthleteGroup.competitor.athleteNumber == athleteNumber
        }?.result
        return if (leaderTime == null || athleteTime == null) {
            0
        } else {
            val x = athleteTime.seconds.toDouble()
            val y = leaderTime!!.seconds.toDouble()
            0.coerceAtLeast((100 * (2 - x / y)).toInt())
        }
    }
}

data class GroupSplitResults(val group: Group, val results: List<CompetitorSplitResultInGroup>)


