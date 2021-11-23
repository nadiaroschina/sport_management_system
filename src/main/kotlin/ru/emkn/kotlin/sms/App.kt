package ru.emkn.kotlin.sms

import ru.emkn.kotlin.sms.data.*
import ru.emkn.kotlin.sms.services.CsvParser
import ru.emkn.kotlin.sms.utils.InvalidFileException
import ru.emkn.kotlin.sms.utils.printMessageAboutCancelCompetition
import java.io.File
import java.time.LocalDate

class App(val title: String, val date: LocalDate) {
    private val nameDirectory: String = "$title|$date"
    private val dir = File(nameDirectory)

    fun run(command: Command) {
        when (command) {
            is CommandStart -> processCommandStart(command)
            is CommandResultsAthlete -> processCommandResultsAthlete(command)
            is CommandResults -> processCommandResults(command)
        }
    }

    private fun processCommandStart(command: CommandStart) {
        val data = CsvParser.parseRequests(command.pathsRequests)
        if (data.isEmpty()) {
            printMessageAboutCancelCompetition()
            return
        }
        dir.mkdir()
        TODO("Распределение по группам и жеребьвка")
    }

    private fun processCommandResultsAthlete(command: CommandResultsAthlete) {
        if (command.pathsProtocolsCheckpoint.isNotEmpty() || command.pathsProtocolsStart.isNotEmpty()) {
            checkExistDir()
        }
        if (command.pathsProtocolsCheckpoint.isEmpty()) {
            val dataCheckpoints = processStream()
        } else {
            TODO("Распарсить pathsProtocolsCheckpoint по путям")
        }

        if (command.pathsProtocolsStart.isEmpty()) {
            TODO("Распарсить command.pathsProtocolsStart из папки")
        } else {
            TODO("Распарсить command.pathsProtocolsStart по путям")
        }

    }

    private fun processCommandResults(command: CommandResults) {
        TODO()
    }

    private fun processStream() {
        val input = mutableListOf<String>()
        var line = readLine()
        while (line != null){
            input.add(line)
            line = readLine()
        }

    }

    private fun checkExistDir() {
        if (!dir.exists()) {
            throw InvalidFileException(nameDirectory)
        }
    }

}