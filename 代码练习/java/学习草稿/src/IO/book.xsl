<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:b="http://example.com/books">
    <xsl:template match="/">
        <html>
            <body>
                <h2>图书列表</h2>
                <table border="1">
                    <tr>
                        <th>ID</th>
                        <th>书名</th>
                        <th>作者</th>
                        <th>价格</th>
                    </tr>
                    <xsl:for-each select="b:books/b:book">
                        <tr>
                            <td><xsl:value-of select="@id"/></td>
                            <td><xsl:value-of select="b:name"/></td>
                            <td><xsl:value-of select="b:author"/></td>
                            <td><xsl:value-of select="b:price"/></td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>