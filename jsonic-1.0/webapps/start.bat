@echo off

java -jar ../lib/winstone-0.9.10.jar ^
--webappsDir=. ^
--commonLibFolder=./lib ^
--preferredClassLoader=winstone.classLoader.WebappDevLoader
