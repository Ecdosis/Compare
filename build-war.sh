#!/bin/bash
if [ ! -d compare ]; then
  mkdir compare
  if [ $? -ne 0 ] ; then
    echo "couldn't create compare directory"
    exit
  fi
fi
if [ ! -d compare/WEB-INF ]; then
  mkdir compare/WEB-INF
  if [ $? -ne 0 ] ; then
    echo "couldn't create compare/WEB-INF directory"
    exit
  fi
fi
if [ ! -d compare/WEB-INF/lib ]; then
  mkdir compare/WEB-INF/lib
  if [ $? -ne 0 ] ; then
    echo "couldn't create compare/WEB-INF/lib directory"
    exit
  fi
fi
rm -f compare/WEB-INF/lib/*.jar
cp dist/Compare.jar compare/WEB-INF/lib/
cp web.xml compare/WEB-INF/
jar cf compare.war -C compare WEB-INF
echo "NB: you MUST copy the contents of tomcat-bin to \$tomcat_home/bin"
