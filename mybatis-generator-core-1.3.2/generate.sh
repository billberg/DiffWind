#! /bin/sh
echo "generating..."
java -jar ./mybatis-generator-core-1.3.2.jar -configfile ./generatorConfig-pg.xml -overwrite
echo "finished"
