#!/bin/sh

#  ----------------------------------------------------------
#
#    OpenRemote, the Home of the Digital Home.
#    Copyright 2008-2014, OpenRemote Inc.
#
#    See the contributors.txt file in the distribution for a
#    full listing of individual contributors.
#
#    This program is free software; you can redistribute it and/or
#    modify it under the terms of the GNU Affero General Public License
#    as published by the Free Software Foundation; either version 3 of
#    the License, or (at your option) any later version.
#
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#    GNU Affero General Public License for more details.
#
#    You should have received a copy of the GNU Affero General Public
#    License along with this program; if not, see http://www.gnu.org/licenses/.
#
#  ----------------------------------------------------------
#
##
#  Convenience script to run project build targets. Use:
#
#    > sh project.sh --help
#
#  to view usage details.
#
#  Author: Juha Lindfors (juha@openremote.org)
#
##

function printHelp()
{
  echo "";
  echo "-------------------------------------------------------------";
  echo "";
  echo "  OpenRemote, the Home of the Digital Home.";
  echo "  Copyright 2008-2014, OpenRemote Inc.";
  echo "";
  echo "  Executes OpenRemote project build targets.";
  echo "";
  echo "-------------------------------------------------------------";
  echo "";
  echo "  Usage: ";
  echo "";
  echo "    sh project.sh";
  echo "";
  echo "         - Runs the default unit-test target of this project.";
  echo "";
  echo "    sh project.sh update";
  echo "";
  echo "         - Updates this project build template.";
  echo "";
  echo "    sh project.sh [-p, -projecthelp]";
  echo "";
  echo "         - Lists all available project build targets.";
  echo "";
  echo "    sh project.sh [target]";
  echo "";
  echo "         - Executes a project build target.";
  echo "";
  echo "    sh project.sh [target] [-Dproperty=value]...";
  echo "";
  echo "         - Executes project build target with given property values.";
  echo "";
  echo "    sh project.sh [target] -verbose";
  echo "";
  echo "         - Verbose output useful to troubleshoot build issues.";
  echo "";
  echo "";

  exit 0;
}

if [ "$1" == "help" -o "$1" == "-h" -o "$1" == "--help" ]; then
  printHelp;
fi

if [ "$1" == "update" ]; then
  ant -f build/project/auto-updated/do-not-modify/project-update.xml $@
else
  ant -f build.xml $@;
  echo "";
  echo "";
fi

