echo on "开始生成..."
java -jar .\mybatis-generator-core-1.3.2.jar -configfile .\generatorConfig-pg.xml -overwrite
echo off 