#!/usr/bin/perl -w

#######################################################################
# Register the security filters
#######################################################################

assign_values();

$OUT = ">$OUTFILE";
open OUT or die "couldn't open $OUTFILE\n";
open FILE or die "couldn't open $FILE\n";


$what2 = "</operation>";
$replace2 ="
<parameter name=\"InflowSecurity\">
   <action>
     <items>Signature</items>
     <passwordCallbackClass>net.exogeni.orca.nodeagent.client.PWCallbackHandlerClient</passwordCallbackClass>
     <signaturePropFile>PROPFILE</signaturePropFile>
     <signatureKeyIdentifier>DirectReference</signatureKeyIdentifier>
   </action>
</parameter>

<parameter name=\"OutflowSecurity\">
    <action>
      <items>Signature</items>
      <passwordCallbackClass>net.exogeni.orca.nodeagent.client.PWCallbackHandlerClient</passwordCallbackClass>
      <signaturePropFile>PROPFILE</signaturePropFile>
      <items>Signature</items>
      <user>actorKey</user>
      <signatureKeyIdentifier>DirectReference</signatureKeyIdentifier>
    </action>
</parameter>

</operation>";

while(<FILE>) {
    $_ =~ s/$what2/$replace2/;
    printf OUT $_;
} 

close OUT;

sub assign_values {
   if ($#ARGV < 1) {
     die "inputfile outputfile\n";
   }
   $FILE = shift @ARGV;
   $OUTFILE = shift @ARGV;
}
