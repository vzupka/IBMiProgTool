echo ===========================================================================
echo --- Create executable Jar file IBMiProgTool.jar
echo ===========================================================================

echo The following command gets the directory in which the script is placed

script_dir=$(dirname $0)
echo script_dir=$("dirname" $0)
echo $script_dir

echo -------------------------------------------------------------
echo The following command makes the application directory current

cd $script_dir
echo cd $script_dir

echo -------------------------------------------------------------------
echo The following command creates the Jar file in the current directory

echo jar cvfm  IBMiProgTool.jar  manifestIBMiProgTool.txt  -C build/classes copyfiles/MainWindow.class  -C build/classes copyfiles
jar cvfm  IBMiProgTool.jar  manifestIBMiProgTool.txt  -C build/classes copyfiles/MainWindow.class  -C build/classes copyfiles

echo -------------------------------------------
echo The following command executes the Jar file

echo java -jar IBMiProgTool.jar
java -jar IBMiProgTool.jar