// **********************************************************************
// FILE   : FolderScanner.scala
// SUBJECT: Class that handles the creation of a file tree from a specified folder.
// AUTHOR : Peter C. Chapin <PChapin@vtc.vsc.edu>
//
// **********************************************************************
package edu.vtc.matchmaker

// TODO: Probably should switch the whole program over to use java.nio. This would avoid unnecessary conversions.
import java.io.{FileNotFoundException, FileReader, BufferedReader, File}
import java.nio.file.{Files, FileSystems}

/**
 * Exception thrown during folder scanning to report scanning errors of various kinds.
 */
class FolderScannerException(message: String) extends Exception(message)

/**
 * Scan a folder hierarchy and build a representation of all the files and folders it contains.
 *
 * @param topLevelName The name of the folder that serves as the root of the hierarchy. This name can be an absolute or
 * relative path.
 * @param honorExclusions If true then exclusions mentioned in the exclusion file will be processed. When scanning a
 * source folder you most likely want exclusions honored. However, when scanning a destination folder you probably
 * don't. That way excluded files that exist on the destination (from a previous run?) will be removed.
 */
class FolderScanner(topLevelName: String, honorExclusions: Boolean) {
  import collection.mutable.ArrayBuffer

  // Read the exclusions file.
  private val exclusionsBuffer = new ArrayBuffer[String]()
  try {
    val input = new BufferedReader(new FileReader("exclusions.txt"))
    var line = ""
    try {
      while ({ line = input.readLine(); line != null }) {
        if (line.length > 0 && line.charAt(0) != '#') exclusionsBuffer.append(line)
      }
    }
    finally {
      input.close()
    }
  }
  catch {
    case _ : FileNotFoundException =>
      // Do nothing. A missing exclusion file is not an error.
  }

  // Normalize the file separators in the exclusions file to the host platform's standard.
  private val exclusions = exclusionsBuffer.toArray
  private def normalizePath(path: String) = path map { ch => if (ch == '/' || ch == '\\') File.separatorChar else ch }
  for (i <- 0 until exclusions.length) exclusions(i) = normalizePath(exclusions(i))

  /**
   * This map is the result of scanning. It maps file names relative to the root of the hierarchy to java.io.File
   * objects. It is important that this map stores names relative to the root. Otherwise maps from different
   * FolderScanners can't be compared. Iterating over this map will return the contents of a folder before the folder
   * itself. The top level folder is not a part of the map.
   */
  val resultMap = new java.util.LinkedHashMap[String, File]

  /**
   * Processes a folder and all subfolders recursively.
   *
   * @param fullName The actual path to the folder to process as understood by the host system.
   */
  private def processName(fullName: String) {
    val folderFile = new File(fullName)
    val directoryContents = folderFile.list

    // Certain odd folders can't be read (permission problem?). In that case list returns null.
    if (directoryContents == null) return

    for (name <- directoryContents) {
      val containedName = fullName + File.separator + name
      // Ignore certain files as defined by the exclusions list (if we are supposed to honor the exclusions list).
      if ( !honorExclusions || !(exclusions exists { _ == containedName }) ) {
        val containedFile = new File(folderFile, name)
        val containedPath = FileSystems.getDefault.getPath(containedFile.getAbsolutePath)

        // Ignore symbolic links. Symbolic links should be copied when going Unix to Unix but they don't work going Unix
        // to Windows.
        if (!Files.isSymbolicLink(containedPath)) {

          // It is important to process a directory before adding its name to the map. That way when iterating the map a
          // directory will appear after its contents.
          //
          if (containedFile.isDirectory) {
            processName(containedName)
          }

          // Here I assume that topLevelName is a prefix of containedName.
          resultMap.put(containedName.substring(topLevelName.length), containedFile)
        }
      }
    }
  }

  // -----------
  // Constructor
  // -----------

  if (topLevelName.endsWith(File.separator)) {
    throw new FolderScannerException("Top level name (" + topLevelName + ") has bad syntax")
  }
  
  private val topLevelFile = new File(topLevelName)
  
  if (!topLevelFile.isDirectory) {
    throw new FolderScannerException("Top level name (" + topLevelName + ") does not name a folder")
  }
  
  processName(topLevelName)
}
