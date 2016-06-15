// **********************************************************************
// FILE   : Main.scala
// SUBJECT: Main program of the MatchMaker application
// AUTHOR : Peter C. Chapin <PChapin@vtc.vsc.edu>
//
// This program synchronizes two folder hierarchies.
// **********************************************************************
package edu.vtc.matchmaker

/**
 * The main object of the application.
 */
object Main {

  /**
   * @param args The command line arguments. The first argument is the name of the source folder. The second argument
   * is the name of the destination folder.
   */
  def main(args: Array[String]) {
    if (args.length != 2) {
      println("Usage: MatchMaker source-folder-name target-folder-name")
      System.exit(1)
    }
    // TODO: Make sure the source and destination areas are not overlapping.

    // Scan source and destination folders and obtain the (Name, File) mappings for each.
    println("Building file lists...")
    println("    Scanning source: " + args(0))
    val sourceScanner = new FolderScanner(args(0), honorExclusions = true)

    println("    Scanning target: " + args(1))
    val destinationScanner = new FolderScanner(args(1), honorExclusions = false)
    println("")

    val sourceMap = sourceScanner.resultMap
    val destinationMap = destinationScanner.resultMap

    // Remove unnecessary files/folders from destination.
    println("Deleting files in target...")
    TreeProcessor.deleteDestinationNames(sourceMap, destinationMap, args(1))
    println("")

    // Update new files/folders to the destination.
    println("Copying/updating files from source to target...")
    TreeProcessor.copySourceNames(sourceMap, destinationMap, args(1))
    println("")
  }

}
