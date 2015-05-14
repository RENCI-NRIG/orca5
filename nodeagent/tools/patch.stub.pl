#!/usr/bin/perl -w

#######################################################################
# Set's the SO_Timeout
#######################################################################

assign_values();

$OUT = ">$OUTFILE";
open OUT or die "couldn't open $OUTFILE\n";
open FILE or die "couldn't open $FILE\n";

$what2 = "targetEndpoint\\)\\);";
$replace2 ="targetEndpoint));
_serviceClient.getOptions().setTimeOutInMilliSeconds(1000000000);";

while(<FILE>) {
    $_ =~ s/$what2/$replace2/;
    printf OUT $_;
} 

sub assign_values {
   if ($#ARGV < 1) {
     die "inputfile outputfile\n";
   }
   $FILE = shift @ARGV;
   $OUTFILE = shift @ARGV;
}

#_serviceClient.getOptions().setProperty(org.apache.axis2.transport.http.HTTPConstants.SO_TIMEOUT, new Integer(0));";
