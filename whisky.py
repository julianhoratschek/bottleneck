from dataclasses import dataclass
from enum import StrEnum
from typing import ClassVar


@dataclass
class WhiskyBase:
    default_string: ClassVar[str] = "Keine Angabe"
    name: str


@dataclass
class Whisky(WhiskyBase):
    """Contains well-formed whiskey-data to transform into text-format
    :ivar type: Single Malt, Grain, Blend etc.
    :ivar region: Where does the Whisky come from?
    :ivar distillery: Who makes the Whisky?
    :ivar age: How long did it age?
    :ivar abv: Alcohol-content
    :ivar casks: Was it aged in special casks?
    :ivar chill_filtered: ' ', 'X' or '?' to indicate chill filtering
    :ivar coloured: ' ', 'X' or '?' to indicate colouring"""

    type: str = WhiskyBase.default_string

    region: str = WhiskyBase.default_string
    distillery: str = WhiskyBase.default_string
    age: str = WhiskyBase.default_string
    abv: str = WhiskyBase.default_string
    casks: str = WhiskyBase.default_string
    chill_filtered: str = "?"
    coloured: str = "?"

    @staticmethod
    def _or_empty(t: str):
        return WhiskyBase.default_string if t.strip(" ") == "" else t

    def _get_chill_filtered(self) -> str:
        """Returns appropriate string depending on self.chill_filtered"""

        match self.chill_filtered:
            case " ":
                return "Nicht kühlgefiltert"
            case "X" | "x":
                return "Kühlgefiltert"
            case "?":
                return "Ggf. kühlgefiltert"
        return "UNBEKANNT"

    def _get_coloured(self) -> str:
        """Returns appropriate string depending on self.coloured"""

        match self.coloured:
            case " ":
                return "ohne Farbstoff"
            case "X" | "x":
                return "mit Zuckercouleur"
            case "?":
                return "Ggf. mit Farbstoff"
        return "UNBEKANNT"

    def to_tex_str(self) -> str:
        """Generates insert for tex-file from this instance. Assumes, the tex-file will include whisxy.sty"""

        return (f"\\whiskey{{{self._or_empty(self.name)}}}"
                f"{{{self._or_empty(self.type)}}}"
                f"{{{self._or_empty(self.abv.replace(',', '.'))}}}"
                f"{{{self._get_chill_filtered()}, {self._get_coloured()}}}"
                f"{{{self._or_empty(self.distillery.strip('[]'))} ({self._or_empty(self.region)})}}"
                f"{{{self._or_empty(self.age)}}}"
                f"{{{self._or_empty(self.casks)}}}")


@dataclass
class WhiskyError(WhiskyBase):
    """Contains ill-formed whiskey-data with an error message"""
    class ErrorType(StrEnum):
        Unknown = "Unknown Error"
        FileNotFound = "File does not exist"
        NoPatternMatch = "File does not match pattern"

    error_type: ErrorType = ErrorType.Unknown

    def __str__(self):
        return f"[!!] For dataset {self.name}: {self.error_type}"
