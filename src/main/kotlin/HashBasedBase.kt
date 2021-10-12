import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Объект HashBasedBase реализует операции с базой данных
 */
object HashBasedBase {
    /**
     * Переменные, содержащие имена файлов с данными
     */
    private val hashTable = RandomAccessFile(File("./kvdbData/hashLinks.txt"), "rw")
    private val keyLists = RandomAccessFile(File("./kvdbData/keyLists.txt"), "rw")
    private val values = RandomAccessFile(File("./kvdbData/values.txt"), "rw")
    private val readInfo = File("./kvdbData/info.txt")
    private val readSize = File("./kvdbData/size.txt")
    private val readUnused = File("./kvdbData/unused.txt")

    /**
     * Переменные, содержащие информацию о базе данных
     * size - количество записей (в том числе удалённых)
     * hashMod - количество элементов хэш-таблицы
     * unused - количество удалённых записей
     * limit - максимальное допустимое среднее количество элементов с одинковым hashCode
     * linkLength - длина ссылки в хэш-таблице
     */
    var size = readSize.readText(Charset.defaultCharset()).drop(5).toInt()
    private var hashMod = readInfo.readText(Charset.defaultCharset()).drop(8).toInt()
    private var unused = readUnused.readText(Charset.defaultCharset()).drop(7).toInt()
    private const val limit = 5
    private const val linkLength = 14L

    /**
     * Метод get возвращает значение по заданному ключу (или null, если его не существует)
     */
    fun get(key: String): String? {
        val beginOfKeyList = getKeyList(key)
        if (beginOfKeyList == 0L) return null

        keyLists.seek(beginOfKeyList)
        val linkToValue = getKeyFromList(key, beginOfKeyList)
        if (linkToValue == null) return null

        values.seek(linkToValue)
        val line = values.readLine()
        val deleted = line.take(1)
        val value = line.drop(2)
        return if (deleted == "0") value else null
    }

    /**
     * Метод add проверяет наличие ключа key в базе
     * Если его нет, добавляет значение value и возвращает true
     * Иначе возвращает false
     */
    fun add(key: String, value: String): Boolean {
        if (get(key) != null) return false

        val hash = getHash(key, hashMod)
        val linkToKeyList = getKeyList(key)
        append(hashTable, "${padLong(keyLists.length())}\n")
        val linkToValue = values.length()
        append(keyLists, "$key $linkToValue $linkToKeyList\n")
        append(values, "0 $value\n")

        setNewSize(size + 1)
        if (size / hashMod > limit) {
            garbageClear()
            expand()
        }
        return true
    }

    /**
     * Метод delete удаляет ключ из базы
     * Возвращает true, если он существовал
     */
    fun delete(key: String): Boolean {
        val beginOfKeyList = getKeyList(key)
        if (beginOfKeyList == 0L) return false

        val linkToValue = getKeyFromList(key, beginOfKeyList)
        if (linkToValue == null) return false

        values.seek(linkToValue)
        val deleted = values.readLine().split(" ")[0]
        return if (deleted == "1") {
            false
        } else {
            values.seek(linkToValue)
            values.writeBytes("1")
            setNewUnused(unused + 1)
            if (unused > size / 2) garbageClear()
            true
        }
    }

    /**
     * Метод replace заменяет значение существующего ключа
     */
    fun replace(key: String, value: String) {
        delete(key)
        add(key, value)
    }

    /**
     * Класс KeyNode хранит информацию о ключе (ключ, ссылки на значение и на следующий элемент в списке)
     */
    private data class KeyNode(val key: String, val linkValue: Long, val linkUp: Long)

    /**
     * Метод expand удваивает хэш-таблицу
     */
    fun expand() {
        // Подготовка временных файлов
        val newMod = hashMod * 2
        val hashTableCopy = RandomAccessFile(File("./kvdbData/copiedHashLinks.txt"), "rw")
        val keyListsCopy = RandomAccessFile(File("./kvdbData/copiedKeyLists.txt"), "rw")
        setHashTableCopy(newMod)
        setKeyListsCopy()

        // Копирование ключей
        for (hash in 0 until hashMod) {
            hashTable.seek(hash * (linkLength + 1))
            val linkToKeyList = hashTable.readLine().toLong()
            if (linkToKeyList == 0L) continue

            val keys = getListKeysFrom(linkToKeyList)

            // Запись ключей в новый файл
            for ((key, linkToValue, linkUp) in keys) {
                // Установка новой ссылки на начало списка
                val newHash = getHash(key, newMod)
                val linkToKey = keyListsCopy.length()
                hashTableCopy.seek(newHash * (linkLength + 1))
                val linkParent = hashTableCopy.readLine().toLong()
                hashTableCopy.seek(newHash * (linkLength + 1))
                hashTableCopy.writeBytes("${padLong(linkToKey)}\n")

                // Запись ключа
                append(keyListsCopy, "$key $linkToValue $linkParent\n")
            }
        }

        // Перезапись файлов
        copyTo("./kvdbData/copiedHashLinks.txt", "./kvdbData/hashLinks.txt")
        copyTo("./kvdbData/copiedKeyLists.txt", "./kvdbData/keyLists.txt")
        setNewMod(newMod)
    }

    /**
     * Метод garbageClear удаляет из файла values все значения, отмеченные как deleted
     */
    fun garbageClear() {
        // Подготовка временных файлов
        val valuesCopy = RandomAccessFile(File("./kvdbData/copiedValues.txt"), "rw")
        val hashTableCopy = RandomAccessFile(File("./kvdbData/copiedHashLinks.txt"), "rw")
        val keyListsCopy = RandomAccessFile(File("./kvdbData/copiedKeyLists.txt"), "rw")
        setHashTableCopy(hashMod)
        setKeyListsCopy()
        setValuesCopy()

        // Запись значений
        for (hash in 0 until hashMod) {
            // Поиск спика ключей, соответствующих hash
            hashTable.seek(hash * (linkLength + 1))
            val linkToKeyList = hashTable.readLine().toLong()
            val keys = uniqueKeys(getListKeysFrom(linkToKeyList))

            // Копирование ключей из списка keys
            var linkParent = 0L
            for ((key, linkValue, linkUp) in keys) {
                values.seek(linkValue)
                if (values.readLine()[0] == '1')
                    continue // Игнорирование удалённой записи

                // Новая запись в хэш-таблице
                val newHash = getHash(key, hashMod)
                val linkToKey = keyListsCopy.length()
                hashTableCopy.seek(newHash * (linkLength + 1))
                hashTableCopy.writeBytes("${padLong(linkToKey)}\n")

                // Копирование
                keyListsCopy.seek(keyListsCopy.length())
                append(keyListsCopy, "$key ${valuesCopy.length()} $linkParent\n")
                linkParent = linkToKey

                values.seek(linkValue)
                append(valuesCopy, "${values.readLine()}\n")
            }
        }

        copyTo("./kvdbData/copiedHashLinks.txt", "./kvdbData/hashLinks.txt")
        copyTo("./kvdbData/copiedKeyLists.txt", "./kvdbData/keyLists.txt")
        copyTo("./kvdbData/copiedValues.txt", "./kvdbData/values.txt")

        setNewSize(size - unused)
        setNewUnused(0)
    }

    /**
     * Метод reset сбрасывает базу данных
     */
    fun reset() {
        setNewMod(4)
        setNewSize(0)
        setNewUnused(0)

        setHashTableCopy(hashMod)
        setKeyListsCopy()
        setValuesCopy()

        copyTo("./kvdbData/copiedHashLinks.txt", "./kvdbData/hashLinks.txt")
        copyTo("./kvdbData/copiedKeyLists.txt", "./kvdbData/keyLists.txt")
        copyTo("./kvdbData/copiedValues.txt", "./kvdbData/values.txt")
    }

    /**
     * Метод getKeyList возвращает указатель на начало списка, в котором хранится ключ
     */
    private fun getKeyList(key: String): Long {
        val hash = getHash(key, hashMod)
        hashTable.seek(hash * (linkLength + 1))
        return hashTable.readLine().toLong()
    }

    /**
     * Метод getKeyFromList возвращает ключ из заданного списка. Если такого нет, возвращает null
     */
    private fun getKeyFromList(key: String, link: Long): Long? {
        keyLists.seek(link)
        while (true) {
            val (keyFromBase, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
            if (keyFromBase == key) return linkValueStr.toLong()
            if (linkUpStr.toLong() == 0L) return null
            keyLists.seek(linkUpStr.toLong())
        }
    }

    /**
     * Метод getHash возвращает хэш строки по модулю mod
     */
    private fun getHash(key: String, mod: Int): Int {
        val hash = key.hashCode() % mod
        return if (hash < 0) hash + mod else hash
    }

    /**
     * Метод append дописывает к файлу строку line
     */
    private fun append(file: RandomAccessFile, line: String) {
        file.seek(file.length())
        file.writeBytes(line)
    }

    /**
     * Метод padLong приводит заданное число к 14-значному десятичному формату
     */
    private fun padLong(x: Long): String = x.toString().padStart(linkLength.toInt(), '0')

    /**
     * Методы setNewMod, setNewSize, setNewUnused изменяют значения соответствующих параметров (в том числе в файлах на диске)
     */
    private fun setNewMod(newMod: Int) {
        hashMod = newMod
        val writer = RandomAccessFile(readInfo, "rw")
        writer.seek(8)
        writer.setLength(8)
        writer.writeBytes(hashMod.toString())
    }

    private fun setNewSize(newSize: Int) {
        size = newSize
        val writer = RandomAccessFile(readSize, "rw")
        writer.seek(5)
        writer.setLength(5)
        writer.writeBytes(size.toString())
    }

    private fun setNewUnused(newUnused: Int) {
        unused = newUnused
        val writer = RandomAccessFile(readUnused, "rw")
        writer.seek(7)
        writer.setLength(7)
        writer.writeBytes(unused.toString())
    }

    /**
     * Метод copyTo копирует данные в заданный файл
     */
    private fun copyTo(fileName: String, copyName: String): RandomAccessFile {
        val copy = File(copyName)
        val file = File(fileName)
        val reader = RandomAccessFile(file, "r")
        val writer = RandomAccessFile(copy, "rw")

        writer.setLength(0)
        writer.seek(0)
        while (reader.filePointer != reader.length()) {
            writer.writeBytes(reader.readLine())
            writer.writeBytes("\n")
        }
        return writer
    }

    /**
     * Методы setHashTableCopy, setKeyListsCopy, setValuesCopy очищают файлы для копирования соответствующих данных
     */
    private fun setHashTableCopy(newMod: Int) {
        val file = RandomAccessFile(File("./kvdbData/copiedHashLinks.txt"), "rw")
        file.setLength(0)
        file.seek(0)
        repeat(newMod) { file.writeBytes("${padLong(0)}\n") }
    }

    private fun setKeyListsCopy() {
        val file = RandomAccessFile(File("./kvdbData/copiedKeyLists.txt"), "rw")
        file.setLength(0)
        file.seek(0)
        file.writeBytes("key linkValue linkUp\n")
    }

    private fun setValuesCopy() {
        val file = RandomAccessFile(File("./kvdbData/copiedValues.txt"), "rw")
        file.setLength(0)
    }

    /**
     * Метод getListKeysFrom возвращает список ключей, начиная с заданного адреса
     */
    private fun getListKeysFrom(link: Long): List<KeyNode> {
        val keys: MutableList<KeyNode> = mutableListOf()
        var linkToNextKey = link
        while (linkToNextKey != 0L) {
            keyLists.seek(linkToNextKey)
            val (key, linkValueStr, linkUpStr) = keyLists.readLine().split(" ")
            keys.add(KeyNode(key, linkValueStr.toLong(), linkUpStr.toLong()))
            linkToNextKey = linkUpStr.toLong()
        }
        return keys.toList()
    }

    /**
     * Метод uniqueKeys оставляет в списке только уникальные ключи
     */
    private fun uniqueKeys(keys: List <KeyNode>): List<KeyNode> {
        val unq: MutableList<KeyNode> = mutableListOf()
        val used: MutableSet<KeyNode> = mutableSetOf()
        keys.forEach {
            if (it !in used) unq.add(it)
            used.add(it)
        }
        return unq
    }
}