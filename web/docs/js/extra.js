window.MathJax = {
    showProcessingMessages: false,
    messageStyle: "none",
    tex2jax: {
        inlineMath: [['$', '$']],
        displayMath: [['$$', '$$']],
        processEscapes: true
    },
    TeX: {
        TagSide: "right",
        TagIndent: ".8em",
        MultLineWidth: "80%",
        multiLine: true,
        equationNumbers: {
            autoNumber: "AMS",
        },
    unicode: {
        fonts: "STIXGeneral,'Arial Unicode MS'"
        },
        Macros: {
            RR: '{\\bf R}',
            bold: ['{\\bf #1}', 1],
        }
    },
    "HTML-CSS": {
        showMathMenu: false,
        scale: 50,
        EqnChunk: 50,
        EqnChunkFactor: 1.5,
        EqnChunkDelay: 100,
        noReflows: false,

    },
    displayAlign: "center",
};

MathJax.Queue(["Typeset", MathJax]);