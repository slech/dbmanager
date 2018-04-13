package de.slech.dbmanager.converter;

import javax.persistence.AttributeConverter;

/**
 * Beispiel für einen Typkonverter. Dieser Converter wandelt ein Enum in einen String um, den Enum Namen.
 * Die Rückrichtung convertToEntityAttribute wird nicht benötigt und ist daher nicht implementiert (ohne Typinfo über
 * das Enum ist das auch nicht möglich.
 */
public class EnumToNameConverter implements AttributeConverter<Enum<?>, String> {

    @Override
    public String convertToDatabaseColumn(Enum<?> anEnum) {
        return anEnum.name();
    }

    @Override
    public Enum<?> convertToEntityAttribute(String s) {
        throw new UnsupportedOperationException("Nicht möglich");
    }
}
