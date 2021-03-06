package ru.emkn.kotlin.sms.services

import ru.emkn.kotlin.sms.classes.TAthlete
import ru.emkn.kotlin.sms.classes.TAthletes
import ru.emkn.kotlin.sms.classes.TCheckpoint
import ru.emkn.kotlin.sms.classes.TCheckpointProtocol
import ru.emkn.kotlin.sms.classes.TCheckpointProtocolToCompetitorData
import ru.emkn.kotlin.sms.classes.TCheckpoints
import ru.emkn.kotlin.sms.classes.TCheckpointsProtocols
import ru.emkn.kotlin.sms.classes.TCheckpointsProtocolsToCompetitorsData
import ru.emkn.kotlin.sms.classes.TCompetition
import ru.emkn.kotlin.sms.classes.TCompetitions
import ru.emkn.kotlin.sms.classes.TCompetitor
import ru.emkn.kotlin.sms.classes.TCompetitorData
import ru.emkn.kotlin.sms.classes.TCompetitors
import ru.emkn.kotlin.sms.classes.TCompetitorsData
import ru.emkn.kotlin.sms.classes.TDistance
import ru.emkn.kotlin.sms.classes.TDistances
import ru.emkn.kotlin.sms.classes.TDistancesToCheckpoints
import ru.emkn.kotlin.sms.classes.TDurationAtCheckpointsToResultsGroupSplit
import ru.emkn.kotlin.sms.classes.TGroup
import ru.emkn.kotlin.sms.classes.TGroups
import ru.emkn.kotlin.sms.classes.TRank
import ru.emkn.kotlin.sms.classes.TRanks
import ru.emkn.kotlin.sms.classes.TResultsGroup
import ru.emkn.kotlin.sms.classes.TResultsTeam
import ru.emkn.kotlin.sms.classes.TSplitsResultsGroup
import ru.emkn.kotlin.sms.classes.TTeam
import ru.emkn.kotlin.sms.classes.TTeams
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import ru.emkn.kotlin.sms.*
import ru.emkn.kotlin.sms.classes.*
import java.io.File
import java.time.LocalTime

data class CheckpointRecord(val competitorNumber: Int, val checkpoint: String, val timeMeasurement: LocalTime)

interface DatabaseInterface {

    // =================================================================================================================

    // получить сущность соревнования по названию
    fun getCompetition(title: String): TCompetition?

    // получить CheckpointTime из соответствующей сущности в бд
    fun tCheckpointProtocolToCheckpointTime(tCheckpointProtocol: TCheckpointProtocol): CheckpointTime

    // получить CompetitorData из соответствующей сущности в бд
    fun tCompetitorDataToCompetitorData(tCompetitorData: TCompetitorData): CompetitorData?

    // получить данные о всех участниках
    fun getCompetitors(): List<Pair<Int, Competitor>>

    // =================================================================================================================

    // получить записи о всех чекпоинтах
    fun getCheckpoints(): List<CheckpointRecord>?

    // добавить запись об одном чекпоинте
    fun insertCheckpointOf(record: CheckpointRecord): Boolean

    // =================================================================================================================

    // загрузка данных конфигурационного файла в базу данных
    fun insertConfigData(): TCompetition

    // загрузка данных конфигурационного файла из базы данных
    fun installConfigData(competitionId: Int)

    // добавление одной дистанции в базу данных
    fun insertDistanceOf(
        title: String,
        distanceType: DistanceType,
        amountCheckpoints: Int,
        checkpoints: List<String>
    ): Boolean

    // добавление одной группы участников в базу данных
    fun insertGroupOf(title: String, distance: String): Boolean

    // удаление одной группы участников из базы данных
    fun deleteGroupOf(title: String): Boolean

    // изменение одной группы участников
    fun updateGroupOf(title: String, newDistance: String): Boolean

    // =================================================================================================================

    // добавление атлетов и команд в базу данных
    fun insertApplications(competition: TCompetition, applications: List<Team>)

    // добавление одной команды
    fun insertTeamOf(title: String): Boolean

    // добавление одного спортсмена
    fun insertAthleteOf(athlete: Athlete): Int?

    // =================================================================================================================

    // добавление участников соревнования
    fun insertCompetitors(data: List<CompetitorsGroup>): Boolean

    // удаление участников соревнования
    fun deleteCompetitors(): Boolean

    // получение данных по всем участникам
    fun getCompetitorData(): List<CompetitorData>

    // =================================================================================================================

    // установить значения поля removed в бд
    fun setRemovedValues(data: List<CompetitorResultInGroup>)

    // =================================================================================================================

    // проверка наличия стартовых протоколов
    fun checkStartsProtocols(competitionId: Int): Boolean

    // проверка наличия результатов групп
    fun checkResultsGroup(competitionId: Int): Boolean

    // =================================================================================================================

    // получить список команд с участниками из бд
    fun getTeamsWithAthletes(): List<Team>?

    // получить список команд из бд
    fun getTeams(): List<TTeam>?

    // получить список спортсменов и их id в таблице
    fun getAthletes(): List<Pair<Int, Athlete>>?

    // получить спортсмена из сущности спортсмена в бд
    fun athleteFromTAthlete(tAthlete: TAthlete): Athlete

    // получить участника из сущности участника в бд
    fun competitorFromTCompetitor(tCompetitor: TCompetitor): Competitor
}

class GeneralDatabase : DatabaseInterface {

    private val dbPath = "database/competitions"
    private val db: Database


    // создание базы данных: подключение файла с базой данных и создание логгера
    init {
        db = connect()
        transaction {
            addLogger(StdOutSqlLogger)
        }
    }


    // подключить базу данных и загрузить еще не созданные таблицы
    private fun connect(): Database {
        val isExist = File(dbPath).exists()
        val database = Database.connect(url = "jdbc:h2:./${dbPath}", driver = "org.h2.Driver")
        if (!isExist) {
            transaction {
                SchemaUtils.create(
                    TCompetitions,
                    TGroups,
                    TRanks,
                    TCheckpoints,
                    TDistances,
                    TDistancesToCheckpoints,
                    TTeams,
                    TAthletes,
                    TCompetitors,
                    TCompetitorsData,
                    TCheckpointsProtocols,
                    TCheckpointsProtocolsToCompetitorsData,
                    TResultsGroup,
                    TSplitsResultsGroup,
                    TDurationAtCheckpointsToResultsGroupSplit,
                    TResultsTeam
                )
            }
        }
        return database
    }

    // получить спортсмена из сущности спортсмена в бд
    override fun athleteFromTAthlete(tAthlete: TAthlete) = Athlete(
        surname = tAthlete.surname, name = tAthlete.name, tAthlete.birthYear,
        Group(TGroup.find { TGroups.id eq tAthlete.groupId }.first().group),
        Rank(TRank.find { TRanks.id eq tAthlete.rankId }.first().rank),
        TTeam.find { TTeams.id eq tAthlete.teamId }.first().team
    )

    // получить участника из сущности участника в бд
    override fun competitorFromTCompetitor(tCompetitor: TCompetitor): Competitor {
        val athlete = athleteFromTAthlete(tCompetitor.getTAthlete())
        return Competitor(tCompetitor.competitorNumber, LocalTime.parse(tCompetitor.startTime), athlete)
    }

    // получить сущность соревнования по названию
    override fun getCompetition(title: String): TCompetition? {
        var competition: TCompetition? = null
        transaction {
            val query = TCompetition.find { TCompetitions.eventName eq title }.limit(1)
            if (!query.empty()) {
                competition = query.first()
            }
        }
        return competition
    }

    // получить записи о всех чекпоинтах
    override fun getCheckpoints(): List<CheckpointRecord>? {
        val res = mutableListOf<CheckpointRecord>()
        transaction {
            TCheckpointProtocol.all().forEach { tCheckpointProtocol ->
                val checkpointString = TCheckpoint.findById(tCheckpointProtocol.checkpointId)?.checkpoint
                    ?: throw IllegalStateException("getCheckpoints: no such checkpoint in the database")
                val timeMeasurement = tCheckpointProtocol.timeMeasurement
                val tCompetitorData =
                    TCompetitorData.all().find { it.checkpointProtocol.contains(tCheckpointProtocol) }
                        ?: throw IllegalStateException("getCheckpoints: no such competitorData in the database")
                val tCompetitor = tCompetitorData.getTCompetitor()
                val record = CheckpointRecord(
                    tCompetitor.competitorNumber,
                    checkpointString,
                    LocalTime.parse(timeMeasurement, TimeFormatter)
                )
                res.add(record)
            }
        }
        return res.ifEmpty { null }
    }

    // получить CheckpointTime из соответствующей сущности в бд
    override fun tCheckpointProtocolToCheckpointTime(tCheckpointProtocol: TCheckpointProtocol): CheckpointTime {
        var checkpointTime: CheckpointTime? = null
        transaction {
            val checkpointId = tCheckpointProtocol.checkpointId
            val checkpoint = TCheckpoint.findById(checkpointId)?.checkpoint ?: return@transaction
            val timeMeasurement = LocalTime.parse(tCheckpointProtocol.timeMeasurement)
            checkpointTime = CheckpointTime(checkpoint, timeMeasurement)
        }
        if (checkpointTime == null) {
            throw IllegalStateException("TCheckpointProtocolToCheckpointTime: no checkpoint for checkpoint protocol found")
        } else {
            return checkpointTime!!
        }
    }

    // получить CompetitorData из соответствующей сущности в бд
    override fun tCompetitorDataToCompetitorData(tCompetitorData: TCompetitorData): CompetitorData? {
        var competitorData: CompetitorData? = null
        transaction {
            val competitorId = tCompetitorData.competitorId
            val isRemoved = tCompetitorData.isRemoved
            val checkpointProtocols = tCompetitorData.checkpointProtocol.toList()
            val competitor = TCompetitor.findById(competitorId)
                ?: return@transaction
            val checkpointsList = checkpointProtocols.map { tCheckpointProtocolToCheckpointTime(it) }
            competitorData = CompetitorData(competitorFromTCompetitor(competitor), checkpointsList, isRemoved)
        }
        return competitorData
    }

    // получение данных по всем участникам
    override fun getCompetitorData(): List<CompetitorData> {
        val res = mutableListOf<CompetitorData>()
        transaction {
            TCompetitorData.all().forEach { tCompetitorData ->
                if (tCompetitorData.getCompetitionId().value == COMPETITION_ID) {
                    val competitorData = tCompetitorDataToCompetitorData(tCompetitorData)
                    if (competitorData != null) {
                        res.add(competitorData)
                    }
                }
            }
        }
        return res
    }

    // добавить один чекпоинт в базу данных
    override fun insertCheckpointOf(record: CheckpointRecord): Boolean {
        var result = false
        val competitorNumber = record.competitorNumber
        val checkpointString = record.checkpoint
        val timeMeasurementString = record.timeMeasurement.format(TimeFormatter)
        var tCompetitorData: TCompetitorData? = null
        var tCheckpointProtocol: TCheckpointProtocol? = null
        transaction {

            val competitorQuery = TCompetitor.find { TCompetitors.competitorNumber eq competitorNumber }.limit(1)
            if (competitorQuery.empty()) {
                throw IllegalStateException("insertCheckpointOf: empty competition query")
            }
            val competitor = competitorQuery.first()

            val competitorDataQuery = TCompetitorData.find { TCompetitorsData.competitorId eq competitor.id }.limit(1)
            if (competitorDataQuery.empty()) {
                tCompetitorData = TCompetitorData.new {
                    competitorId = competitor.id
                    isRemoved = false
                }
            } else {
                tCompetitorData = competitorDataQuery.first()
            }

            val checkpointQuery = TCheckpoint.find { TCheckpoints.checkpoint eq checkpointString }.limit(1)
            if (checkpointQuery.empty()) {
                throw IllegalStateException("insertCheckpointOf: empty checkpoint query")
            }
            val checkpoint = checkpointQuery.first()
            tCheckpointProtocol = TCheckpointProtocol.new {
                competitorId = competitor.id
                checkpointId = checkpoint.id
                timeMeasurement = timeMeasurementString
            }
        }
        transaction {
            if (tCheckpointProtocol != null && tCompetitorData != null) {
                TCheckpointProtocolToCompetitorData.new {
                    checkpointProtocolId = tCheckpointProtocol!!.id
                    competitorDataId = tCompetitorData!!.id
                }
                result = true
            }
        }
        return result
    }

    // получить данные о всех участниках
    override fun getCompetitors(): List<Pair<Int, Competitor>> {
        val result = mutableListOf<Pair<Int, Competitor>>()
        transaction {
            val competition = TCompetition.findById(COMPETITION_ID)
                ?: throw IllegalStateException("getCompetitors: no such competition")
            TCompetitor.all().forEach { tCompetitor ->
                val tCompetitorCompetitionId = tCompetitor.getCompetitionId()
                if (tCompetitorCompetitionId == competition.id) {
                    val competitor = competitorFromTCompetitor(tCompetitor)
                    result.add(Pair(tCompetitor.id.value, competitor))
                }
            }
        }
        return result
    }

    // получить список команд из бд
    override fun getTeams(): List<TTeam>? {
        var teams: List<TTeam>? = null
        transaction {
            val query = TTeam.find { TTeams.competitionId eq COMPETITION_ID }
            if (!query.empty()) {
                teams = query.toList()
            }
        }
        return teams
    }

    // получить список спортсменов и их id в таблице
    override fun getAthletes(): List<Pair<Int, Athlete>>? {
        var athletes: List<Pair<Int, Athlete>>? = null

        transaction {
            val query = TAthlete.find { TAthletes.competitionId eq COMPETITION_ID }
            if (query.empty()) {
                return@transaction
            }
            athletes = query.toList().map {
                it.id.value to athleteFromTAthlete(it)
            }
        }
        return athletes
    }

    // загрузка данных конфигурационного файла в базу данных
    override fun insertConfigData(): TCompetition {
        lateinit var competition: TCompetition
        transaction {
            competition = TCompetition.new {
                eventName = EVENT_NAME
                sport = EVENT_SPORT
                date = EVENT_DATE_STRING
                time = EVENT_TIME_STRING
            }
            RANK_NAMES.forEach {
                TRank.new {
                    competitionId = competition.id
                    rank = it
                }
            }
            DISTANCE_CRITERIA.forEach { (it_distance, criteria) ->
                TDistance.new {
                    competitionId = competition.id
                    distance = it_distance
                    type = criteria.distanceType
                    checkpointsCount = criteria.checkpointsCount
                }
            }
            GROUP_DISTANCES.forEach { (it_group, it_distance) ->
                val distanceReference =
                    TDistance.find { (TDistances.distance eq it_distance) and (TDistances.competitionId eq competition.id) }
                        .limit(1).first()
                TGroup.new {
                    competitionId = competition.id
                    group = it_group
                    distanceId = distanceReference.id
                }
            }
            CHECKPOINTS_LIST.forEach { it_checkpoint ->
                TCheckpoint.new {
                    competitionId = competition.id
                    checkpoint = it_checkpoint
                }
            }
            DISTANCE_CRITERIA.forEach { (distance, criteria) ->
                val distanceReference =
                    TDistance.find { (TDistances.distance eq distance) and (TDistances.competitionId eq competition.id) }
                        .limit(1).first()
                criteria.checkpointsOrder.forEach { checkpoint ->
                    if (checkpoint.isNotEmpty()) {
                        val checkpointReference =
                            TCheckpoint.find { (TCheckpoints.checkpoint eq checkpoint) and (TCheckpoints.competitionId eq competition.id) }
                                .limit(1)
                        TDistancesToCheckpoints.insert {
                            it[distanceId] = distanceReference.id
                            it[checkpointId] = checkpointReference.first().id
                        }
                    }
                }
            }
        }
        return competition
    }

    // загрузка данных конфигурационного файла из базы данных
    override fun installConfigData(competitionId: Int) {
        transaction {
            val dataGroupTable = TGroup.find { TGroups.competitionId eq competitionId }
            val dataDistanceGroup = TDistance.find { TDistances.competitionId eq competitionId }
            RANK_NAMES = TRank.find { TRanks.competitionId eq competitionId }.mapTo(mutableListOf()) { it.rank }
            GROUP_NAMES = dataGroupTable.mapTo(mutableListOf()) { it.group }
            CHECKPOINTS_LIST = TCheckpoint.find { TCheckpoints.competitionId eq competitionId }
                .mapTo(mutableListOf()) { it.checkpoint }
            GROUP_DISTANCES = dataGroupTable.associateTo(mutableMapOf()) {
                it.group to TDistance.find { (TDistances.id eq it.distanceId) and (TDistances.competitionId eq competitionId) }
                    .limit(1).first().distance
            }
            DISTANCE_CRITERIA = dataDistanceGroup.associateTo(mutableMapOf()) { distanceData ->
                val checkpoints = distanceData.checkpoints.map { it.checkpoint }
                distanceData.distance to when (distanceData.type) {
                    DistanceType.FIXED -> FixedRoute(checkpoints)
                    DistanceType.CHOICE -> ChoiceRoute(distanceData.checkpointsCount, checkpoints)
                }
            }
        }

    }

    // добавление одной дистанции в базу данных
    override fun insertDistanceOf(
        title: String,
        distanceType: DistanceType,
        amountCheckpoints: Int,
        checkpoints: List<String>
    ): Boolean {
        var result = true
        transaction {
            try {
                val tCheckpointsList = checkpoints.map {
                    TCheckpoint.find {
                        (TCheckpoints.checkpoint eq it) and (
                                TCheckpoints.competitionId eq COMPETITION_ID)
                    }.first()
                }
                val competition = TCompetition.findById(COMPETITION_ID) ?: return@transaction
                val tDistance = TDistance.new {
                    competitionId = competition.id
                    distance = title
                    type = distanceType
                    checkpointsCount = amountCheckpoints

                }
                tDistance.checkpoints = SizedCollection(tCheckpointsList)
            } catch (e: Exception) {
                LOGGER.debug { e }
                result = false
            }

        }
        return result
    }

    // добавление одной группы участников в базу данных
    override fun insertGroupOf(title: String, distance: String): Boolean {
        var result = false
        transaction {
            val query =
                TDistance.find { (TDistances.distance eq distance) and (TDistances.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (query.empty()) {
                return@transaction
            }
            val distanceData = query.first()
            val competition = TCompetition.findById(COMPETITION_ID) ?: return@transaction
            TGroup.new {
                competitionId = competition.id
                group = title
                distanceId = distanceData.id
            }
            result = true
        }
        LOGGER.debug { "Database: insertGroupOf | $result" }
        return result
    }

    // удаление одной группы участников из базы данных
    override fun deleteGroupOf(title: String): Boolean {
        var result = false
        transaction {
            val query =
                TGroup.find { (TGroups.group eq title) and (TGroups.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (query.empty()) {
                return@transaction
            }
            val group = query.first()
            group.delete()
            result = true
        }
        LOGGER.debug { "Database: deleteGroupOf | $result" }
        return result
    }

    // изменение одной группы участников
    override fun updateGroupOf(title: String, newDistance: String): Boolean {
        var result = false
        transaction {
            val distanceQuery =
                TDistance.find { (TDistances.distance eq newDistance) and (TDistances.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (distanceQuery.empty()) {
                return@transaction
            }
            val distanceData = distanceQuery.first()
            val groupQuery =
                TGroup.find { (TGroups.group eq title) and (TGroups.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (groupQuery.empty()) {
                return@transaction
            }
            result = true
            val groupData = groupQuery.first()
            groupData.distanceId = distanceData.id
        }
        LOGGER.debug { "Database: insertGroupOf | $result" }
        return result
    }

    // проверка наличия стартовых протоколов
    override fun checkStartsProtocols(competitionId: Int): Boolean = getTeamsWithAthletes() != null

    // проверка наличия результатов групп
    override fun checkResultsGroup(competitionId: Int): Boolean = getCheckpoints() != null

    // получить список команд с участниками из бд
    override fun getTeamsWithAthletes(): List<Team>? {
        var teams: List<Team>? = null
        transaction {
            teams = getTeams()?.map {
                Team(
                    it.team,
                    TAthlete.find { (TAthletes.teamId eq it.id) and (TAthletes.competitionId eq COMPETITION_ID) }
                        .map { tAthlete -> athleteFromTAthlete(tAthlete) })
            }
        }
        return teams
    }

    // добавление атлетов и команд в базу данных
    override fun insertApplications(competition: TCompetition, applications: List<Team>) {
        transaction {
            applications.forEach { application ->
                TTeam.new {
                    competitionId = competition.id
                    team = application.teamName
                }
                application.athletes.forEach { athlete ->

                    val groupReference: TGroup =
                        TGroup.find { TGroups.group eq athlete.group.groupName }.limit(1).first()
                    val rankReference: TRank =
                        TRank.find { TRanks.rank eq (athlete.rank.rankName ?: "") }.limit(1).first()
                    val teamReference: TTeam =
                        TTeam.find { TTeams.team eq (athlete.teamName) }.limit(1).first()

                    TAthlete.new {
                        competitionId = competition.id
                        name = athlete.name
                        surname = athlete.surname
                        birthYear = athlete.birthYear
                        groupId = groupReference.id
                        rankId = rankReference.id
                        teamId = teamReference.id
                    }
                }
            }
        }
    }

    // добавление одной команды
    override fun insertTeamOf(title: String): Boolean {
        var result = false
        transaction {
            val query =
                TTeam.find { (TTeams.team eq title) and (TTeams.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (!query.empty()) {
                return@transaction
            }
            val competition = TCompetition.findById(COMPETITION_ID) ?: return@transaction
            TTeam.new {
                competitionId = competition.id
                team = title
            }
            result = true
        }
        LOGGER.debug { "Database: insertTeamOf | $result" }
        return result
    }

    // добавление одного спортсмена
    override fun insertAthleteOf(athlete: Athlete): Int? {
        var athleteId: Int? = null
        transaction {
            val athleteQuery =
                TAthlete.find { (TAthletes.name eq athlete.name) and (TAthletes.surname eq athlete.surname) and (TAthletes.competitionId eq COMPETITION_ID) }
            if (!athleteQuery.empty()) {
                return@transaction
            }
            val competition = TCompetition.findById(COMPETITION_ID) ?: return@transaction

            val groupQuery =
                TGroup.find { (TGroups.group eq athlete.group.groupName) and (TGroups.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (groupQuery.empty()) {
                return@transaction
            }
            val newGroup = groupQuery.first()

            val rankName = athlete.rank.rankName ?: ""
            val rankQuery =
                TRank.find { (TRanks.rank eq rankName) and (TRanks.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (rankQuery.empty()) {
                return@transaction
            }
            val newRank = rankQuery.first()

            val teamQuery =
                TTeam.find { (TTeams.team eq athlete.teamName) and (TTeams.competitionId eq COMPETITION_ID) }
                    .limit(1)
            if (teamQuery.empty()) {
                return@transaction
            }
            val newTeam = teamQuery.first()

            athleteId = TAthlete.new {
                competitionId = competition.id
                name = athlete.name
                surname = athlete.surname
                birthYear = athlete.birthYear
                groupId = newGroup.id
                rankId = newRank.id
                teamId = newTeam.id
            }.id.value
        }
        LOGGER.debug { "Database: insertAthleteOf | $athleteId" }
        return athleteId
    }

    // добавление участников соревнования
    override fun insertCompetitors(data: List<CompetitorsGroup>): Boolean {
        var result = false
        transaction {
            data.forEach { competitorsGroup ->
                competitorsGroup.competitors.forEach { competitor ->
                    val athleteQuery = TAthlete.find {
                        (TAthletes.competitionId eq COMPETITION_ID) and (TAthletes.surname eq competitor.surname)
                    }
                    if (athleteQuery.empty()) {
                        throw IllegalStateException("Athlete ${competitor.surname} is not stored in database")
                    }
                    val athleteReference = athleteQuery.first()
                    TCompetitor.new {
                        athleteId = athleteReference.id
                        competitorNumber = competitor.athleteNumber
                        startTime = competitor.startTime.toString()
                    }
                }
                result = true
            }
        }
        return result
    }

    // удаление участников соревнования
    override fun deleteCompetitors(): Boolean {
        var result = false
        transaction {
            val competition = TCompetition.findById(COMPETITION_ID)
                ?: return@transaction
            TCompetitor.all().forEach { tCompetitor ->
                if (tCompetitor.getCompetitionId() == competition.id) {
                    tCompetitor.delete()
                }
                result = true
            }
        }
        return result
    }

    // установить значения поля removed в бд
    override fun setRemovedValues(data: List<CompetitorResultInGroup>) {
        transaction {
            data.forEach { competitorResultInGroup ->
                if (competitorResultInGroup.result == null) {
                    val competitor = competitorResultInGroup.competitor
                    val tCompetitor = TCompetitor.all().find {
                        (it.getTAthlete().competitionId.value == COMPETITION_ID) &&
                                (it.getTAthlete().surname == competitor.surname) &&
                                (it.getTAthlete().name == competitor.name)
                    } ?: throw IllegalStateException("setRemovedValues: competitor not found in database")
                    val queryCompetitorData =
                        TCompetitorData.find { TCompetitorsData.competitorId eq tCompetitor.id }.limit(1)
                    if (queryCompetitorData.empty()) {
                        throw IllegalStateException("setRemovedValues: competitorData not found in database")
                    }
                    val tCompetitorData = queryCompetitorData.first()
                    tCompetitorData.isRemoved = true
                }
            }
        }
    }

}
