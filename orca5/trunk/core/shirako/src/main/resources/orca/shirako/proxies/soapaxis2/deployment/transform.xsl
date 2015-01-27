<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:axis="http://xml.apache.org/axis/wsdd/">

  <xsl:param name="propFile" />
  <xsl:output method="xml"/>

  <xsl:template match="signaturePropFile">
    <xsl:copy><xsl:value-of select="$propFile"/></xsl:copy>
  </xsl:template>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*" > 
        <xsl:attribute name='{name()}'> <xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
