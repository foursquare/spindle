# Copyright 2015 Foursquare Labs Inc. All Rights Reserved.

java_library(
  name = 'binary',
  sources = globs('*.scala'),
  dependencies = [
    '3rdparty:commons-io',
    '3rdparty:joda-time',
    '3rdparty:parboiled-scala',
    '3rdparty:thrift',
    '3rdparty:rogue-field',
    '3rdparty:scalate',
    'src/main/scala/com/foursquare/spindle/codegen/runtime',
    'src/main/scala/com/foursquare/spindle/codegen/parser',
    'src/main/scala/com/foursquare/spindle/runtime',
    'src/main/ssp/codegen',
  ],
)


jvm_binary(
  name = 'spindle',
  main = 'com.foursquare.spindle.codegen.binary.ThriftCodegen',
  dependencies = [
    ':binary',
  ],
)
