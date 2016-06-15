// *********************************************************************
// FILE   : TreeProcessor.scala
// SUBJECT: Helper object that contains methods for manipulating the file trees.
// AUTHOR : Peter C. Chapin <PChapin@vtc.vsc.edu>
//
// *********************************************************************
package edu.vtc.matchmaker
import java.io.File

/**
 * Object that collects together various methods for processing the trees computed by a FolderScanner. Note that these
 * "trees" are really stored in LinkedHashMaps such that iterating over the map visits each file in the hierarchy.
 * Iteration visits a folder's contents before visiting the folder itself.
 */
object TreeProcessor {

  /**
   * Dumps information in a tree for debugging purposes.
   *
   * @param fileMap The tree to dump.
   */
  def dumpNames(fileMap: java.util.LinkedHashMap[String, File]) {
    val nameSet = fileMap.keySet
    val it = nameSet.iterator
    while (it.hasNext) {
      val itemName = it.next()
      val itemFile = fileMap.get(itemName)
      print(itemName)
      print(if (itemFile.isDirectory) "  DIR: " else "  FIL: ")
      print(if (itemFile.isHidden) "H " else "- ")
      print("size = " + itemFile.length)
      print("; last_modified = " + itemFile.lastModified)
      println("\n")
    }
  }


  /**
   * Remove files/folders from the destination that are not present in the source.
   *
   * @param sourceMap The representation of the source folder hierarchy.
   * @param destinationMap The representation of the destination folder hierarchy.
   * @param destinationTopLevelName The name of the root of the destination hierarchy.
   */
  def deleteDestinationNames(sourceMap: java.util.LinkedHashMap[String, File],
                             destinationMap: java.util.LinkedHashMap[String, File],
                             destinationTopLevelName: String) {

    // This code depends on folder names appearing after their contents in the destination map. If an entire folder is
    // to be deleted that will cause the contents to be deleted first.
    //
    val destinationNameSet = destinationMap.keySet
    val destinationIterator = destinationNameSet.iterator
    while (destinationIterator.hasNext) {
      val destinationName = destinationIterator.next()
      val destinationFile = destinationMap.get(destinationName)
      if (!sourceMap.containsKey(destinationName)) {
        print("deleting " + destinationTopLevelName + destinationName)
        if (!destinationFile.delete()) {
          print(" DELETE FAILED!")
        }
        print("\n")
      }
    }
  }


  /**
   * Copy a source file to a destination file.
   *
   * @param source The source file to copy.
   * @param destination The destination file to where the copy will be made.
   */
  private def copyFile(source: String, destination: String) = {
    import java.nio.file._

    val sourcePath = FileSystems.getDefault.getPath(source)
    val destinationPath = FileSystems.getDefault.getPath(destination)
    try {
      Files.copy(
        sourcePath,
        destinationPath,
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS)
    }
    catch {
      case ex: FileSystemException =>
        println(s"\nFAILED to copy: ${ex.getMessage}")
    }
  }


  /**
   * Creates a new file in the destination area by copying the given source file. If the source file is a folder, it is
   * created in the destination by this method.
   *
   * @param sourceName The name of the source file relative to the source hierarchy.
   * @param sourceFile The File object representing the source file.
   * @param destinationTopLevelName The name of the root of the destination hierarchy.
   */
  private def createDestinationFile(sourceName: String,
                                    sourceFile: File,
                                    destinationTopLevelName: String) {

    // TODO: Because folders get created before the files they contain, the last modified time on the folder ends up
    // being the current time rather than that of the source folder. This can be fixed, if it matters, by running
    // MatchMaker a second time.
    //
    val fullDestinationName = destinationTopLevelName + sourceName
    print("creating " + fullDestinationName)
    val destinationFile = new File(fullDestinationName)
    if (sourceFile.isDirectory) {
      destinationFile.mkdir()
    }
    else {
      copyFile(sourceFile.getPath, fullDestinationName)
    }
    print("\n")
  }


  /**
   * Update the destination to be a copy of the source. This method only applies for destination files that already
   * exist. If the destination does not exist createDestinationFile must be used instead.
   *
   * @param sourceFile The source file to copy.
   * @param destinationFile The existing destination file to update.
   */
  private def updateDestinationFile(sourceFile: File, destinationFile: File) {

    // TODO: This code does not behave well if the source and destination both exist and one is
    // a file while the other is a directory.
    //
    val fullDestinationName = destinationFile.getPath
    if (sourceFile.isFile && destinationFile.isFile) {
      print("updating " + fullDestinationName)
      copyFile(sourceFile.getPath, fullDestinationName)
      print("\n")
    }
  }


  /**
   * Copies files in the source hierarchy that need to be created or updated in the destination hierarchy.
   *
   * @param sourceMap The source hierarchy to copy.
   * @param destinationMap The destination hierarchy where the copy will be made.
   * @param destinationTopLevelName The name of the root of the destination hierarchy.
   */
  def copySourceNames(sourceMap: java.util.LinkedHashMap[String, File],
                      destinationMap: java.util.LinkedHashMap[String, File],
                      destinationTopLevelName: String) {

    // This code depends on folder names appearing after their contents in the source map. If an entire folder is to be
    // copied that will cause the folder itself to be copied first before the contents (due to the reverse iteration).
    //
    val sourceNames = sourceMap.keySet.toArray
    val it = sourceNames.reverseIterator
    while (it.hasNext) {
      val sourceName = it.next().asInstanceOf[String]
      val sourceFile = sourceMap.get(sourceName)
      if (!destinationMap.containsKey(sourceName)) {
        // The source file does not exist in the destination.
        createDestinationFile(sourceName, sourceFile, destinationTopLevelName)
      }
      else {
        // The source file does exist in the destination. Does it need to be updated?
        val destinationFile = destinationMap.get(sourceName)
        if (sourceFile.length       != destinationFile.length     ||
            sourceFile.lastModified != destinationFile.lastModified) {
          updateDestinationFile(sourceFile, destinationFile)
        }
      }
    }
  }

}

