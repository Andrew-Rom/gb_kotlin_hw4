/*
KOTLIN - HW4
------------
Продолжаем дорабатывать домашнее задание из предыдущего семинара.
За основу берём код решения из предыдущего домашнего задания.

— Добавьте новую команду export, которая экспортирует добавленные значения в текстовый файл в формате JSON.
  Команда принимает путь к новому файлу. Например: export /Users/user/myfile.json
— Реализуйте DSL (предметно ориентированный язык) на Kotlin, который позволит конструировать JSON
  и преобразовывать его в строку.
— Используйте этот DSL для экспорта данных в файл.
— Выходной JSON не обязательно должен быть отформатирован, поля объектов могут идти в любом порядке.
  Главное, чтобы он имел корректный синтаксис.
  Такой вывод тоже принимается:
[{"emails": ["ew@huh.ru"],"name": "Alex","phones": ["34355","847564"]},{"emails": [],"name": "Tom","phones": ["84755"]}]

Записать текст в файл можно при помощи удобной функции-расширения writeText:
File("/Users/user/file.txt").writeText("Text to write")

Под капотом она использует такую конструкцию

FileOutputStream(file).use {
it.write(text.toByteArray(Charsets.UTF_8))
}

Пример DSL для создания HTML https://pl.kotl.in/c-mvANGBr
*/

import com.diogonunes.jcolor.Ansi
import com.diogonunes.jcolor.Attribute
import java.io.File
import java.util.*
import kotlin.system.exitProcess

val HELP_MESSAGE: String = """
        Перечень команд:
        
              exit
                - прекращение работы
                
              help
                - справка
                
              add <Имя> phone <Номер телефона>
                - сохранение записи с введенными именем и номером телефона
                - добавление нового номера телефона к уже имеющейся записи соответствующего человека
                 
              add <Имя> email <Адрес электронной почты>
                - сохранение записи с введенными именем и адрес электронной почты
                - добавление нового адреса электронной почты к уже имеющейся записи соответствующего человека
                
              show <Имя>
                - выводит по введенному имени человека связанные с ним телефоны и адреса электронной почты
                
              find <критерий>
                - выводит по введенному критерию (номер телефона или адрес электронной почты) список людей
              
              export </path/file.json>
                - экспорт значений в текстовый файл <file.json> в формате JSON в директории <path>
              
    """.trimIndent()
val COMMON_ERROR_MESSAGE: String =
    Ansi.colorize("Ошибка! Команда введена неверно. Список команд ниже", Attribute.BRIGHT_RED_TEXT())

var phoneBook = mutableMapOf<String, Person>()


sealed interface Command {
    fun execute()
    fun isValid(): Boolean
}

data object ExitCommand : Command {

    override fun execute() {
        exitProcess(0)
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return Ansi.colorize("Введена команда \"exit\"", Attribute.BRIGHT_GREEN_TEXT())
    }
}

data object HelpCommand : Command {

    override fun execute() {
        println(HELP_MESSAGE)
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return Ansi.colorize("Вывод справочной информации", Attribute.BRIGHT_GREEN_TEXT())
    }
}

class AddUserPhoneCommand(private val entryData: List<String>) : Command {

    private val phonePattern = Regex("[+]+\\d+")
    private val entryPhone = entryData[entryData.indexOf("phone") + 1]

    override fun execute() {
        if (phoneBook.containsKey(entryData[0])) {
            phoneBook[entryData[0]]?.contacts?.get("phone")?.add(entryPhone)
        } else {
            val person = Person(
                entryData[0],
                contacts = mutableMapOf(Pair("phone", mutableListOf(entryPhone)), Pair("email", mutableListOf()))
            )
            phoneBook[entryData[0]] = person
        }
    }

    override fun isValid(): Boolean {
        return entryPhone.matches(phonePattern) && entryData.size <= 3
    }

    override fun toString(): String {
        return Ansi.colorize(
            "Введена команда записи нового пользователя ${entryData[0]} с номером телефона $entryPhone",
            Attribute.BRIGHT_GREEN_TEXT()
        )
    }
}

class AddUserEmailCommand(private val entryData: List<String>) : Command {

    private val emailPattern = Regex("[a-zA-z0-9]+@[a-zA-z0-9]+[.]([a-zA-z0-9]{2,4})")
    private val entryEmail = entryData[entryData.indexOf("email") + 1]

    override fun execute() {
        if (phoneBook.containsKey(entryData[0])) {
            phoneBook[entryData[0]]?.contacts?.get("email")?.add(entryEmail)
        } else {
            val person = Person(
                entryData[0],
                contacts = mutableMapOf(Pair("phone", mutableListOf()), Pair("email", mutableListOf(entryEmail)))
            )
            phoneBook[entryData[0]] = person
        }
    }

    override fun isValid(): Boolean {
        return entryEmail.matches(emailPattern) && entryData.size <= 3
    }

    override fun toString(): String {
        return Ansi.colorize(
            "Введена команда записи нового пользователя ${entryData[0]} с адресом электронной почты $entryEmail",
            Attribute.BRIGHT_GREEN_TEXT()
        )
    }
}

class ShowCommand(private val name: String) : Command {
    override fun execute() {
        if (phoneBook.isEmpty()) {
            println("Phonebook is not initialized")
        } else if (phoneBook.containsKey(name)) {
            println(phoneBook[name])
        } else {
            println("Person with name $name was not found")
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return Ansi.colorize("Введена команда \"show\"", Attribute.BRIGHT_GREEN_TEXT())
    }

}

data class Person(
    var name: String,
    var contacts: MutableMap<String, MutableList<String>> = mutableMapOf(
        "phone" to mutableListOf(),
        "email" to mutableListOf()
    )
) {
    override fun toString(): String {
        return buildString {
            append("Пользователь: ")
            append(name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            append(buildString {
                if (contacts["phone"]?.isNotEmpty() == true) {
                    append("\n\t")
                    append("phone(s): ")
                    append(
                        contacts["phone"].toString()
                            .replace("[", "")
                            .replace("]", "")
                    )
                }
            })
            append(buildString {
                if (contacts["email"]?.isNotEmpty() == true) {
                    append("\n\t")
                    append("email(s): ")
                    append(
                        contacts["email"].toString()
                            .replace("[", "")
                            .replace("]", "")
                    )
                }
            })
            append("\n")
        }
    }
}

class FindCommand(private val value: String) : Command {
    override fun execute() {
        val persons = mutableListOf<Person>()
        if (phoneBook.isEmpty()) {
            println("Phonebook is not initialized")
        } else {
            for (person in phoneBook.values) {
                if (person.contacts["phone"]!!.contains(value) or person.contacts["email"]!!.contains(value)) {
                    persons.add(person)
                }
            }
        }
        if (persons.isEmpty()) {
            println("Person with $value was not found")
        } else {
            persons.forEach { person ->
                println(person)
            }
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return Ansi.colorize("Введена команда \"find\"", Attribute.BRIGHT_GREEN_TEXT())
    }

}

class ExportCommand(private val path: String) : Command {
    override fun execute() {
        if (phoneBook.isEmpty()) {
            println("Phonebook is not initialized")
        } else {

            val personsJson = phoneBook.values.map { person ->
                json {
                    addGroup("name", person.name)

                    person.contacts["phone"]?.let { addGroup("phone", it) }
                    person.contacts["email"]?.let { addGroup("email", it) }
                }
            }
            val jsonFile = "[${personsJson.joinToString(", ")}]"
            File(path).writeText(jsonFile)
            println("JSON file $path was created")
        }
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun toString(): String {
        return Ansi.colorize("Введена команда \"export\"", Attribute.BRIGHT_GREEN_TEXT())
    }
}

class MyJson {
    private val obj = mutableMapOf<String, Any>()

    fun addGroup(key: String, value: Any) {
        obj[key] = value
    }

    override fun toString(): String {
        val result = obj.entries.joinToString(",\n    ") { (key, value) ->
            if (value is String) {
                "\"$key\": \"$value\""
            } else if (value is List<*>) {
                "\"$key\": [\n${value.joinToString(",\n") { data -> "${if (data is String) "\"$data\"" else data}" }}\n]"
            } else {
                "\"$key\": $value"
            }
        }
        return "\n{\n    $result\n}\n"
    }
}

fun json(init: MyJson.() -> Unit): MyJson {
    return MyJson().apply(init)
}


fun readCommand(): Command {
    print("> ")
    val entryData: List<String> = readln().lowercase().split(' ')

    return when (entryData[0]) {
        "add" -> {
            if (entryData.size > 3 && "phone" in entryData && "email" !in entryData) {
                AddUserPhoneCommand(entryData.subList(1, entryData.size))
            } else if (entryData.size > 3 && "phone" !in entryData && "email" in entryData) {
                AddUserEmailCommand(entryData.subList(1, entryData.size))
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "show" -> {
            if (entryData.size > 1) {
                ShowCommand(entryData[1])
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "find" -> {
            if (entryData.size > 1) {
                FindCommand(entryData[1])
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "export" -> {
            if (entryData.size > 1) {
                ExportCommand(entryData[1])
            } else {
                println(COMMON_ERROR_MESSAGE)
                HelpCommand
            }
        }

        "help" -> HelpCommand
        "exit" -> ExitCommand
        else -> {
            println(COMMON_ERROR_MESSAGE)
            return HelpCommand
        }
    }
}


fun hw4() {

    println("Введите команду или \"help\" для вывода списка команд ")

    while (true) {
        val command: Command = readCommand()
        if (command.isValid()) {
            println(command)
            command.execute()
        } else {
            println(COMMON_ERROR_MESSAGE)
            println(HELP_MESSAGE)
        }
    }

}
