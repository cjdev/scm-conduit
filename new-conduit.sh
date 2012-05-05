#!/bin/sh

NAME=$1
P4PATH=$2
ME=$3
P4PORT=$4

CONDUITS_HOME=$5
P4_CLIENT_PREFIX="scm-conduit-`hostname`"

CLIENT="$P4_CLIENT_PREFIX-$NAME"

LOCAL_CONDUIT_PATH="$CONDUITS_HOME/$NAME"

DESCRIPTOR="
<scm-conduit-state>
   <last-synced-p4-changelist>1</last-synced-p4-changelist>
   <p4-port>$P4PORT</p4-port>
   <p4-read-user>$ME</p4-read-user>
   <p4-client-id>$CLIENT</p4-client-id>
</scm-conduit-state>"

mkdir $LOCAL_CONDUIT_PATH
echo "$DESCRIPTOR"> $LOCAL_CONDUIT_PATH/.scm-conduit
bzr init $LOCAL_CONDUIT_PATH
 

WORKSPACE="
Client: $CLIENT

Update: 2011/08/16 10:02:27

Access: 2011/08/31 12:03:31

Owner:  $ME

Host:   martin

Description:
        Created by $ME.

Root:   $LOCAL_CONDUIT_PATH

Options:        noallwrite noclobber nocompress unlocked nomodtime normdir

SubmitOptions:  submitunchanged

LineEnd:        local

View:
        $P4PATH/... //$CLIENT/...
"
echo "$WORKSPACE"
echo "$WORKSPACE" | p4 -c $CLIENT -d $LOCAL_CONDUIT_PATH -p $P4PORT -u $ME workspace -i

