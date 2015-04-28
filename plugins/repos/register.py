# Copyright 2015 Foursquare Labs Inc. All Rights Reserved.

from __future__ import absolute_import

import os

from pants.backend.jvm.repository import Repository
from pants.base.build_environment import get_buildroot
from pants.base.build_file_aliases import BuildFileAliases


internal_nexus_host = 'nexus.prod.foursquare.com'


internal_repo = Repository(
  name = 'internal',
  url = 'http://' + internal_nexus_host,
  push_db_basedir = os.path.join(
    get_buildroot(),
    'build-support',
    'pushdb',
  ),
)


def build_file_aliases():
  return BuildFileAliases.create(
    objects={
      'internal_repo': internal_repo,
    },
  )
