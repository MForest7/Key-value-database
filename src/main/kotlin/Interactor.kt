object Interactor {
    fun receiveCommand(): List <String>? {
        val args = readLine()
        return if (args == null) {
            null
        } else {
            val command = args.takeWhile { it != ' ' }
            val params = args.dropWhile { it != ' ' }.drop(1)
            val key = params.takeWhile { it != ' ' }
            val value = params.dropWhile { it != ' ' }.drop(1)
            listOf(command, key, value)
        }
    }

    fun runCommand(args: List<String>?): Boolean {
        if (args != null) {
            if (args.isEmpty()) return true
            when (args[0]) {
                "exit" -> {
                    return false
                }
                "add" -> {
                    if (!checkEnoughArgs(args, 3)) return true
                    if (!HashBasedBase.add(args[1], args[2]))
                        tryReplace(args[1], args[2])
                    return true
                }
                "get" -> {
                    if (!checkEnoughArgs(args, 2)) return true
                    println(HashBasedBase.get(args[1]) ?: "No such key in base")
                    return true
                }
                "del" -> {
                    if (!checkEnoughArgs(args, 2)) return true
                    HashBasedBase.delete(args[1])
                    return true
                }
                "reset" -> {
                    HashBasedBase.reset()
                }
                "clear" -> {
                    HashBasedBase.garbageClear()
                }
                else -> {
                    println("Incorrect format")
                    println("Unknown command: ${args[0]}")
                    return true
                }
            }
        }
        return true
    }

    private fun tryReplace(key: String, value: String): Boolean {
        println("This key has already been created")
        println("Value: ${HashBasedBase.get(key)}")
        println("Do you want to replace value? (y/n)")
        val args = readLine()?.split(" ")
        if (args != null) {
            if (args[0] == "y") {
                HashBasedBase.replace(key, value)
                return true
            }
        }
        return false
    }

    private fun checkEnoughArgs(args: List<String>, size: Int):Boolean {
        if (args.size >= size) return true
        println("Incorrect format")
        println("${args[0]} must have ${size - 1} arguments")
        return false
    }
}