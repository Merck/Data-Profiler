#
# Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
#
#	Licensed to the Apache Software Foundation (ASF) under one
#	or more contributor license agreements. See the NOTICE file
#	distributed with this work for additional information
#	regarding copyright ownership. The ASF licenses this file
#	to you under the Apache License, Version 2.0 (the
#	"License"); you may not use this file except in compliance
#	with the License. You may obtain a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
#
#	Unless required by applicable law or agreed to in writing,
#	software distributed under the License is distributed on an
#	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#	KIND, either express or implied. See the License for the
#	specific language governing permissions and limitations
#	under the License.
#
#!/bin/sh
set -e

die() { exec 2>&1 ; for line ; do echo "$line" ; done ; exit 1 ; }
exists() { which "$1" &> /dev/null ; }

[ -d ~/.fonts ] || die \
	'There is no .fonts directory in your home.' \
	'Is fontconfig set up for privately installed fonts?'

ARCHIVE=PowerPointViewer.exe
URL="https://sourceforge.net/projects/mscorefonts2/files/cabs/$ARCHIVE"

if ! [ -e "$ARCHIVE" ] ; then
	if   exists curl  ; then curl -A '' -LO      "$URL"
	elif exists wget  ; then wget -U ''          "$URL"
	elif exists fetch ; then fetch --user-agent= "$URL"
	else die 'You have neither curl nor wget nor fetch.' \
		'Please manually download this file first:' "$URL"
	fi
fi

TMPDIR=`mktemp -d`
trap 'rm -rf "$TMPDIR"' EXIT INT QUIT TERM

cabextract -L -F ppviewer.cab -d "$TMPDIR" "$ARCHIVE"

cabextract -L -F '*.TT[FC]' -d ~/.fonts "$TMPDIR/ppviewer.cab"

cd ~/.fonts && mv ~/.fonts/cambria.ttc ~/.fonts/cambria.ttf 

ls -lah ~/.fonts

fc-cache -fv ~/.fonts