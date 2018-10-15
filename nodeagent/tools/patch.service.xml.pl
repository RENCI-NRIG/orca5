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
     <passwordCallbackClass>net.exogeni.orca.nodeagent.PWCallbackHandlerServer</passwordCallbackClass>
     <signaturePropFile>service.properties</signaturePropFile>
   </action>
</parameter>

<parameter name=\"OutflowSecurity\">
    <action>
      <items>Signature</items>
       <user>serverkey</user>
       <passwordCallbackClass>net.exogeni.orca.nodeagent.PWCallbackHandlerServer</passwordCallbackClass>
       <signaturePropFile>service.properties</signaturePropFile>
       <signatureKeyIdentifier>DirectReference</signatureKeyIdentifier>
    </action>
</parameter>

</operation>";

while(<FILE>) {
    $_ =~ s/$what2/$replace2/;
    printf OUT $_;
} 

close OUT;

$OUT2 = ">$OUTFILE2";
open OUT2 or die "couldn't open $OUTFILE2\n";
open OUTFILE or die "couldn't open $OUTFILE\n";

$contents = "";

while(<OUTFILE>) {
    $contents = $contents.$_;  
} 


$replace3="registerFirstKeyResponse</outputActionMapping>
</operation>"; 

$replace4="getServiceKeyResponse</outputActionMapping>
</operation>"; 

$contents =~ s/registerAuthorityKeyResponse(.*?)<\/operation>/$replace3/s;
$contents1 = $contents;

$contents1 =~ s/getServiceKeyResponse(.*?)<\/operation>/$replace4/s; 

printf OUT2 $contents1;

sub assign_values {
   if ($#ARGV < 1) {
     die "inputfile outputfile\n";
   }
   $FILE = shift @ARGV;
   $OUTFILE = shift @ARGV;
   $OUTFILE2 = shift @ARGV;
}
