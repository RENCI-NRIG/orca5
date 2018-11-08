<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:axis="http://xml.apache.org/axis/wsdd/">

  <xsl:param name="serviceName" />
  <xsl:output method="xml"/>

  <xsl:template match="axis:service">
    <xsl:copy>
      <xsl:attribute name="name">
        <xsl:value-of select="$serviceName"/>
      </xsl:attribute>

      <xsl:for-each select="@*" > 
        <xsl:if test="name() != 'name'">
          <xsl:attribute name='{name()}'>
            <xsl:value-of select="."/>
          </xsl:attribute>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates />
    </xsl:copy>
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
