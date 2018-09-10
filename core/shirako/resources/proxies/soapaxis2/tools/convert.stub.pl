#!/usr/bin/perl -w

#######################################################################
# Add an extra argument to each method from the port type file.       #
# These are methods that throw java.rmi.RemoteException and have name #
# different from createCall                                           #
#######################################################################

assign_values();

$OUT = ">$OUTFILE";
open OUT or die "couldn't open $OUTFILE\n";
open FILE or die "couldn't open $FILE\n";

# Add an extra argument to all method from the PortType.
# The current heuristic is that these methods
$what = "\\)";
#                    throws java.rmi.RemoteException
$replace = ", net.exogeni.orca.security.AuthToken authToken) throws java.rmi.RemoteException";

#$what1 = "\\_messageContext\\.setEnvelope\\(env\\);";
#$replace1 = "_messageContext.setEnvelope(env);net.exogeni.orca.shirako.proxies.soap.util.ContextTools.setMyAuthToken(_call, authToken);";
while(<FILE>) {
#    printf OUT $_;
    $_ =~ s/$what/$replace/;
#    $_ =~ s/$what1/$replace1/;
    printf OUT $_;
} 

sub assign_values {
   if ($#ARGV < 1) {
     die "inputfile outputfile\n";
   }
   $FILE = shift @ARGV;
   $OUTFILE = shift @ARGV;
}
