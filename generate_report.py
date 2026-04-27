#!/usr/bin/env python3
"""
Generate a formatted PDF report comparing Traditional vs Devin AI migration
for the CardDemo Mainframe Modernization project (2M LOC).

Requirements:
    pip install reportlab

Usage:
    python generate_report.py
"""

from reportlab.lib import colors
from reportlab.lib.pagesizes import letter, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import (
    SimpleDocTemplate,
    Table,
    TableStyle,
    Paragraph,
    Spacer,
    PageBreak,
)


# ---------------------------------------------------------------------------
# Colour palette
# ---------------------------------------------------------------------------
LIGHT_GRAY = colors.HexColor("#F2F2F2")
WHITE = colors.white
HEADER_BG = colors.HexColor("#4472C4")
HEADER_FG = colors.white
TOTAL_BG = colors.HexColor("#D9E2F3")
BLACK = colors.black

# ---------------------------------------------------------------------------
# Styles
# ---------------------------------------------------------------------------
styles = getSampleStyleSheet()

TITLE_STYLE = ParagraphStyle(
    "ReportTitle",
    parent=styles["Title"],
    fontSize=14,
    leading=18,
    spaceAfter=12,
    alignment=1,  # centre
)

SECTION_STYLE = ParagraphStyle(
    "SectionHead",
    parent=styles["Heading2"],
    fontSize=11,
    leading=14,
    spaceBefore=16,
    spaceAfter=6,
    textColor=colors.HexColor("#1F3864"),
)

FOOTNOTE_STYLE = ParagraphStyle(
    "Footnote",
    parent=styles["Normal"],
    fontSize=7,
    leading=9,
    spaceAfter=10,
    textColor=colors.HexColor("#444444"),
)

CELL_STYLE = ParagraphStyle(
    "Cell",
    parent=styles["Normal"],
    fontSize=8,
    leading=10,
)

CELL_BOLD = ParagraphStyle(
    "CellBold",
    parent=CELL_STYLE,
    fontName="Helvetica-Bold",
)

HEADER_CELL = ParagraphStyle(
    "HeaderCell",
    parent=styles["Normal"],
    fontSize=8.5,
    leading=10,
    fontName="Helvetica-Bold",
    textColor=HEADER_FG,
)

ASSUMPTIONS_STYLE = ParagraphStyle(
    "Assumptions",
    parent=styles["Normal"],
    fontSize=7.5,
    leading=10,
)


# ---------------------------------------------------------------------------
# Helper: wrap text in Paragraph for word-wrap inside table cells
# ---------------------------------------------------------------------------
def P(text, style=CELL_STYLE):
    return Paragraph(str(text), style)


def PB(text):
    return Paragraph(f"<b>{text}</b>", CELL_BOLD)


def PH(text):
    return Paragraph(str(text), HEADER_CELL)


# ---------------------------------------------------------------------------
# Table 1 — Project Stage Comparison
# ---------------------------------------------------------------------------
TABLE1_HEADERS = [
    "Project Stage",
    "Trad.\nDuration",
    "Trad.\nFTEs",
    "Trad.\nCost",
    "Devin\nDuration",
    "Devin\nRDEs",
    "Devin\nRDE Cost",
    "Est. ACU\nInstances",
    "Est. ACU\nCost",
    "Effort\nIndex",
    "Efficiency\nGain",
]

TABLE1_DATA = [
    ["Discovery & Analysis",       "3 mo",  "70",  "$3M",  "2 mo",   "35",  "$1M",   "2",  "$0.4M", "25%",  "4.0x"],
    ["Architecture & Schema Design","4 mo",  "66",  "$3M",  "3 mo",   "38",  "$1M",   "2",  "$1M",   "36%",  "2.8x"],
    ["Code Conversion",            "13 mo",  "71", "$11M",  "9 mo",   "66",  "$6M",   "5",  "$5M",   "64%",  "1.6x"],
    ["Data Migration",              "7 mo",  "45",  "$4M",  "6 mo",   "60",  "$4M",   "1",  "$1M",  "113%",  "0.9x"],
    ["Testing",                    "13 mo",  "61", "$10M",  "6 mo",   "44",  "$3M",   "4",  "$3M",   "33%",  "3.0x"],
    ["Deployment & CI/CD",          "3 mo",  "44",  "$2M",  "2 mo",   "19",  "$0.4M", "1",  "$0.2M", "29%",  "3.4x"],
]

TABLE1_TOTAL = [
    "Total",
    "24 mo",
    "110 avg /\n150 peak",
    "$33M",
    "~15 mo",
    "93 avg /\n100 peak",
    "$14M",
    "4 avg /\n5 peak",
    "$10M",
    "53%",
    "1.9x",
]

TABLE1_FOOTNOTE = (
    "Traditional Cost includes labor + proportionally allocated tool license ($11M). "
    "Devin total = RDE Cost ($14M) + ACU Cost ($10M) = $24M. "
    "All values rounded to nearest $1M except where original rounding yielded $0M "
    "(shown to one decimal)."
)

# ---------------------------------------------------------------------------
# Table 2 — Executive Financial Summary
# ---------------------------------------------------------------------------
TABLE2_HEADERS = ["Metric", "Traditional", "Devin", "Net Delta"]

TABLE2_DATA = [
    ["Peak Headcount",          "150 FTE",     "100 RDE",   "\u221250 (\u221233%)"],
    ["Total Accenture COGS",    "$33M",        "$24M",      "\u2212$9M (\u221227%)"],
    ["Fixed Bid Client Price",  "$41M",        "$43M",      "+$2M Premium (Accelerated TTV)"],
    ["Accenture Profit Margin", "$8M (20%)",   "$19M (44%)","+$11M (+24 pp)"],
]

TABLE2_FOOTNOTE = (
    "Traditional margin based on 20% gross margin over COGS. "
    "Devin Fixed Bid includes a $2M premium reflecting accelerated time-to-value "
    "(24 mo \u2192 15 mo). The $2M TTV premium + $9M COGS reduction converts into "
    "$11M additional margin at 44%."
)

# ---------------------------------------------------------------------------
# Assumptions
# ---------------------------------------------------------------------------
ASSUMPTIONS = [
    ["FTE Blended Rate (Traditional)",            "$8,333/PM ($100K/yr) \u2014 60/40 offshore/onshore"],
    ["RDE Rate (Devin-deployed engineer)",         "$12,500/PM ($150K/yr)"],
    ["Devin Project Blended Rate",                 "$10,000/PM ($120K/yr) \u2014 50/50 RDE/standard mix"],
    ["ACU Instance Pool",                          "4,000 ACU per instance"],
    ["ACU Price",                                  "$120,000/month per instance"],
    ["Traditional Tool License",                   "$3.50/LOC conversion + $1.0M/yr runtime + $1.5M vendor PS/maintenance"],
    ["Fixed Bid Margin (Traditional baseline)",    "20% gross margin on COGS"],
    ["Devin TTV Premium",                          "$2M added to Fixed Bid for accelerated delivery (24 \u2192 15 months)"],
    ["Discovery Effort Index",                     "25% (upper boundary \u2014 DeepWiki-driven exhaustive analysis at 2M LOC)"],
    ["Testing Effort Index",                       "33% (upper boundary \u2014 AI bulk test generation advantage at scale)"],
]


# ---------------------------------------------------------------------------
# Build helpers
# ---------------------------------------------------------------------------
def _alternating_row_colours(n_data_rows, header_rows=1):
    """Return TableStyle commands for alternating row background."""
    cmds = []
    for i in range(n_data_rows):
        row = i + header_rows
        bg = LIGHT_GRAY if i % 2 == 0 else WHITE
        cmds.append(("BACKGROUND", (0, row), (-1, row), bg))
    return cmds


def _base_table_style():
    return [
        # Grid
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#CCCCCC")),
        ("BOX", (0, 0), (-1, -1), 1, colors.HexColor("#999999")),
        # Header row
        ("BACKGROUND", (0, 0), (-1, 0), HEADER_BG),
        ("TEXTCOLOR", (0, 0), (-1, 0), HEADER_FG),
        # Alignment
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 3),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
    ]


def build_table1():
    """Return flowable elements for Table 1."""
    header = [PH(h.replace("\n", "<br/>")) for h in TABLE1_HEADERS]
    rows = []
    for r in TABLE1_DATA:
        rows.append([P(c) for c in r])
    # Total row — bold
    total = [PB(c.replace("\n", "<br/>")) for c in TABLE1_TOTAL]
    rows.append(total)

    data = [header] + rows
    n_data = len(TABLE1_DATA)
    total_row_idx = n_data + 1  # 0-indexed header row

    col_widths = [
        1.35 * inch,  # stage
        0.55 * inch,  # trad dur
        0.55 * inch,  # trad fte
        0.55 * inch,  # trad cost
        0.55 * inch,  # devin dur
        0.65 * inch,  # devin rde
        0.6 * inch,   # devin rde cost
        0.65 * inch,  # acu inst
        0.6 * inch,   # acu cost
        0.55 * inch,  # effort idx
        0.65 * inch,  # eff gain
    ]

    style_cmds = _base_table_style()
    style_cmds += _alternating_row_colours(n_data)
    # Total row styling
    style_cmds += [
        ("BACKGROUND", (0, total_row_idx), (-1, total_row_idx), TOTAL_BG),
        ("LINEABOVE", (0, total_row_idx), (-1, total_row_idx), 1.5, BLACK),
    ]
    # Right-align numeric columns (1-10)
    for col in range(1, 11):
        style_cmds.append(("ALIGN", (col, 1), (col, -1), "RIGHT"))

    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(TableStyle(style_cmds))
    return t


def build_table2():
    """Return flowable elements for Table 2."""
    header = [PH(h) for h in TABLE2_HEADERS]
    rows = [[P(c) for c in r] for r in TABLE2_DATA]
    data = [header] + rows

    col_widths = [1.8 * inch, 1.6 * inch, 1.6 * inch, 2.6 * inch]

    style_cmds = _base_table_style()
    style_cmds += _alternating_row_colours(len(TABLE2_DATA))
    for col in range(1, 4):
        style_cmds.append(("ALIGN", (col, 1), (col, -1), "RIGHT"))

    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(TableStyle(style_cmds))
    return t


def build_assumptions_table():
    """Return flowable elements for the Assumptions section."""
    header = [PH("Assumption"), PH("Value")]
    rows = [
        [Paragraph(a, ASSUMPTIONS_STYLE), Paragraph(v, ASSUMPTIONS_STYLE)]
        for a, v in ASSUMPTIONS
    ]
    data = [header] + rows

    col_widths = [3.0 * inch, 5.5 * inch]

    style_cmds = _base_table_style()
    style_cmds += _alternating_row_colours(len(ASSUMPTIONS))

    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(TableStyle(style_cmds))
    return t


# ---------------------------------------------------------------------------
# Page template — add page number footer
# ---------------------------------------------------------------------------
def _page_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 7)
    canvas.setFillColor(colors.HexColor("#888888"))
    page_num = canvas.getPageNumber()
    text = f"Page {page_num}"
    canvas.drawRightString(
        doc.pagesize[0] - 0.5 * inch, 0.4 * inch, text
    )
    canvas.drawString(
        0.5 * inch,
        0.4 * inch,
        "CardDemo Mainframe Modernization \u2014 Migration Comparison Report",
    )
    canvas.restoreState()


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    output_path = "migration_comparison_report.pdf"
    page = landscape(letter)
    doc = SimpleDocTemplate(
        output_path,
        pagesize=page,
        leftMargin=0.5 * inch,
        rightMargin=0.5 * inch,
        topMargin=0.5 * inch,
        bottomMargin=0.6 * inch,
    )

    story = []

    # Title
    story.append(
        Paragraph(
            "CardDemo Mainframe Modernization \u2014 Traditional vs Devin AI "
            "Migration Comparison (2M LOC)",
            TITLE_STYLE,
        )
    )
    story.append(Spacer(1, 6))

    # --- Table 1 ---
    story.append(Paragraph("Table 1 \u2014 Project Stage Comparison", SECTION_STYLE))
    story.append(build_table1())
    story.append(Spacer(1, 4))
    story.append(Paragraph(TABLE1_FOOTNOTE, FOOTNOTE_STYLE))

    # --- Table 2 ---
    story.append(Paragraph("Table 2 \u2014 Executive Financial Summary", SECTION_STYLE))
    story.append(build_table2())
    story.append(Spacer(1, 4))
    story.append(Paragraph(TABLE2_FOOTNOTE, FOOTNOTE_STYLE))

    # --- Assumptions ---
    story.append(Paragraph("Assumptions", SECTION_STYLE))
    story.append(build_assumptions_table())

    doc.build(story, onFirstPage=_page_footer, onLaterPages=_page_footer)
    print(f"Report saved to {output_path}")


if __name__ == "__main__":
    main()
