SRW MX Portable TX48 Extractor by Dashman
-------------------------------------

This program allows you to extract and reinsert TX48 textures inside several BIN files of SRW MX Portable. You'll need to have Java installed in your computer to operate this.

How to extract:

1) Extract the file you want to search for textures to the folder with the program.
2) In a command / shell window (or using a batch / script file), execute this:

java -jar tx48_extract.jar -e <filename>

3) The textures will be generated in a <filename>_extracted subfolder, along with a <filename>_files.list file containing information needed for reinsertion.


How to insert:

1) Put the program, the BIN file, its corresponding LIST file and all the BMP files in the same directory.
2) Execute

java -jar tx48_extract.jar -i <filename>

3) The extracted files will be re-inserted into the file.


IMPORTANT NOTES:

* Don't change the names of files. The program looks for the files it originally extracted, which are listed inside the LIST file. If those files are named differently, they'll be ignored.

* All extrated textures are indexed, and should stay indexed when you re-insert them. You can change the palettes for all files to your liking and will be shown like that ingame, but don't change their palette size.