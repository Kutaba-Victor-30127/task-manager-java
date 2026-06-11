Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$mdPath = "C:\Users\Kutaba\Desktop\java-projects\task-manager-java\Task-Manager-Java-Ghid-Interviu.md"
$docxPath = "C:\Users\Kutaba\Desktop\java-projects\task-manager-java\Task-Manager-Java-Ghid-complet-pentru-interviu-v2.docx"
$tmpRoot = Join-Path $env:TEMP ("docx_" + [guid]::NewGuid().ToString())

$null = New-Item -ItemType Directory -Path $tmpRoot
$null = New-Item -ItemType Directory -Path (Join-Path $tmpRoot "_rels")
$null = New-Item -ItemType Directory -Path (Join-Path $tmpRoot "docProps")
$null = New-Item -ItemType Directory -Path (Join-Path $tmpRoot "word")
$null = New-Item -ItemType Directory -Path (Join-Path $tmpRoot "word\_rels")

function Escape-Xml([string]$text) {
    if ($null -eq $text) { return "" }
    return [System.Security.SecurityElement]::Escape($text)
}

function Make-Run([string]$text) {
    $escaped = Escape-Xml $text
    return "<w:r><w:t xml:space='preserve'>$escaped</w:t></w:r>"
}

function Make-Paragraph([string]$text, [string]$style = "Normal", [bool]$pageBreakBefore = $false) {
    $pPr = "<w:pPr><w:pStyle w:val='$style'/></w:pPr>"
    if ($pageBreakBefore) {
        $pPr = "<w:pPr><w:pStyle w:val='$style'/><w:pageBreakBefore/></w:pPr>"
    }
    return "<w:p>$pPr$(Make-Run $text)</w:p>"
}

function Make-BulletParagraph([string]$text) {
    $escaped = Escape-Xml $text
    return "<w:p><w:pPr><w:ind w:left='720' w:hanging='360'/></w:pPr><w:r><w:t xml:space='preserve'>- </w:t></w:r><w:r><w:t xml:space='preserve'>$escaped</w:t></w:r></w:p>"
}

$lines = Get-Content -Path $mdPath -Encoding UTF8
$paragraphs = New-Object System.Collections.Generic.List[string]
$firstHeading = $true

foreach ($line in $lines) {
    if ($line -match '^#\s+(.+)$') {
        $paragraphs.Add((Make-Paragraph $matches[1] "Heading1" (-not $firstHeading)))
        $firstHeading = $false
        continue
    }
    if ($line -match '^##\s+(.+)$') {
        $paragraphs.Add((Make-Paragraph $matches[1] "Heading2"))
        continue
    }
    if ($line -match '^###\s+(.+)$') {
        $paragraphs.Add((Make-Paragraph $matches[1] "Heading3"))
        continue
    }
    if ($line -match '^\-\s+(.+)$') {
        $paragraphs.Add((Make-BulletParagraph $matches[1]))
        continue
    }
    if ([string]::IsNullOrWhiteSpace($line)) {
        $paragraphs.Add("<w:p/>")
        continue
    }
    $paragraphs.Add((Make-Paragraph $line "Normal"))
}

$documentXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:w10="urn:schemas-microsoft-com:office:word" xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" mc:Ignorable="w14 wp14">
  <w:body>
    $($paragraphs -join "`n    ")
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>
"@

$stylesXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:spacing w:after="120"/></w:pPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:spacing w:before="240" w:after="120"/></w:pPr>
    <w:rPr><w:b/><w:sz w:val="32"/><w:szCs w:val="32"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:spacing w:before="200" w:after="80"/></w:pPr>
    <w:rPr><w:b/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:spacing w:before="160" w:after="60"/></w:pPr>
    <w:rPr><w:b/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr>
  </w:style>
</w:styles>
"@

$contentTypesXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>
"@

$relsXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
"@

$docRelsXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
"@

$created = (Get-Date).ToUniversalTime().ToString("s") + "Z"
$coreXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Task Manager Java - Ghid complet pentru interviu</dc:title>
  <dc:creator>Codex</dc:creator>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">$created</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">$created</dcterms:modified>
</cp:coreProperties>
"@

$appXml = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Codex</Application>
</Properties>
"@

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(([System.IO.Path]::Combine($tmpRoot, "[Content_Types].xml")), $contentTypesXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmpRoot "_rels\.rels"), $relsXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmpRoot "word\document.xml"), $documentXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmpRoot "word\styles.xml"), $stylesXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmpRoot "word\_rels\document.xml.rels"), $docRelsXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmpRoot "docProps\core.xml"), $coreXml, $utf8)
[System.IO.File]::WriteAllText((Join-Path $tmpRoot "docProps\app.xml"), $appXml, $utf8)

[System.IO.Compression.ZipFile]::CreateFromDirectory($tmpRoot, $docxPath)
Remove-Item $tmpRoot -Recurse -Force

Write-Output $docxPath
