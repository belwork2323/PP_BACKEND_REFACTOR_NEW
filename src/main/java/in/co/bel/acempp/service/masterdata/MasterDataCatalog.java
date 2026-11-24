package in.co.bel.acempp.service.masterdata;

import in.co.bel.acempp.dto.masterdata.MasterDataFieldDef;
import in.co.bel.acempp.dto.masterdata.MasterDataTypeDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MasterDataCatalog {

    public static final String CODE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._/\\\\-]*$";
    public static final String NAME_TYPE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 ._/\\\\-]*$";
    public static final String MASTER_DATA_NAME_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 _/\\\\-]*$";
    public static final String OBSERVATION_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 .,_/-]*$";
    public static final String MIXERS = "mixers";
    public static final String BUILDINGS = "buildings";
    public static final String OVENS = "ovens";
    public static final String EQUIPMENT = "equipment";
    public static final String EQUIPMENT_TYPES = "equipment-types";
    public static final String BEAM_ENERGY = "beam-energy";
    public static final String UNITS = "units";
    public static final String CASTING_STATIONS = "casting-stations";
    public static final String SUBSCALE_ARTICLES = "subscale-articles";

    public static final String MATERIALS = "materials";
    public static final String INSULATION_SPECIFICATIONS = "insulation-specifications";
    public static final String MOTOR_STAGES = "motor-stages";
    public static final String DIMENSIONAL_PARAMETERS = "dimensional-parameters";
    public static final String MIXING_CYCLES = "mixing-cycles";
    public static final String CURING_CYCLES = "curing-cycles";
    public static final String QUALITY_CHECKS = "quality-checks";
    public static final String QC_DIVISIONS = "qc-divisions";

    private static final Set<String> NESTED_TYPES = Set.of(
            MATERIALS,
            INSULATION_SPECIFICATIONS,
            MIXING_CYCLES,
            CURING_CYCLES,
            QC_DIVISIONS
    );

    private static final Map<String, MasterDataTypeDescriptor> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put(MIXERS, build(MIXERS, "Mixer Master"));
        TYPES.put(BUILDINGS, build(BUILDINGS, "Building Master"));
        TYPES.put(OVENS, build(OVENS, "Oven Master",
                attrInt("noOfOvenAvailable", "Ovens available", 0, 9999)));
        TYPES.put(EQUIPMENT_TYPES, buildWithTypeNameLabel(EQUIPMENT_TYPES, "NDT Equipment Types Master",
                attrString("typeCode", "Type Code", 32, CODE_PATTERN),
                attrOptionalString("description", "Description", 255 , OBSERVATION_PATTERN)));
        TYPES.put(EQUIPMENT, build(EQUIPMENT, "NDT Equipments Master",
                attrString("equipmentType", "Equipment type", 32, CODE_PATTERN)));
        TYPES.put(BEAM_ENERGY, build(BEAM_ENERGY, "Beam Energy Master",
                attrString("energyUnit", "Energy unit", 32, null)));
        TYPES.put(UNITS, buildWithUnitNameLabel(UNITS, "Unit Master",
                attrString("unitCode", "Unit Code", 16, CODE_PATTERN),
                attrString("symbol", "Symbol", 32, null),
                attrOptionalString("description", "Description", 255 , OBSERVATION_PATTERN)));
        TYPES.put(CASTING_STATIONS, build(CASTING_STATIONS, "Casting Station Master",
                attrString("stationType", "Station type", 64, NAME_TYPE_PATTERN)));
        TYPES.put(SUBSCALE_ARTICLES, build(SUBSCALE_ARTICLES, "Subscale Article Master",
                attrBoolean("isBem", "Is BEM"),
                attrOptionalInt("bemSize", "BEM size", 1, 9999),
                attrOptionalString("bemUnit", "BEM unit", 32)));

        TYPES.put(MATERIALS, buildNested(MATERIALS, "Raw Materials"));
        TYPES.put(INSULATION_SPECIFICATIONS, buildNested(INSULATION_SPECIFICATIONS, "Insulation Specifications"));
        TYPES.put(MOTOR_STAGES, buildMotorStages());
        TYPES.put(DIMENSIONAL_PARAMETERS, build(DIMENSIONAL_PARAMETERS, "Dimensional Parameter Master",
                attrString("projectId", "Project", 128, null, true),
                attrInt("motorType", "Motor stage", 0, 99, true),
                attrServerGeneratedString("paramId", "Parameter ID", 64, CODE_PATTERN),
                attrOptionalNumber("minValue", "Min value"),
                attrOptionalNumber("maxValue", "Max value"),
                attrOptionalString("unit", "Unit", 32)));
        TYPES.put(MIXING_CYCLES, buildNested(MIXING_CYCLES, "Mixing Cycle Master"));
        TYPES.put(CURING_CYCLES, buildNested(CURING_CYCLES, "Curing Cycle Master"));
        // quality-checks merged into Mixing Cycle (premix/finalMixQualityChecks); constant kept for legacy codes.
        TYPES.put(QC_DIVISIONS, buildNested(QC_DIVISIONS, "QC Division Master"));
    }

    private MasterDataCatalog() {
    }

    public static List<MasterDataTypeDescriptor> allTypes() {
        return List.copyOf(TYPES.values());
    }

    public static MasterDataTypeDescriptor get(String type) {
        return TYPES.get(type);
    }

    public static boolean isKnown(String type) {
        return TYPES.containsKey(type);
    }

    public static boolean isNested(String type) {
        return NESTED_TYPES.contains(type);
    }

    private static MasterDataTypeDescriptor build(String type, String label, MasterDataFieldDef... attrs) {
        return buildWithNameLabel(type, label, "Name", attrs);
    }

    private static MasterDataTypeDescriptor buildWithUnitNameLabel(
            String type, String label, MasterDataFieldDef... attrs) {
        return buildWithNameLabel(type, label, "Unit Name", attrs);
    }

    private static MasterDataTypeDescriptor buildWithTypeNameLabel(
            String type, String label, MasterDataFieldDef... attrs) {
        return buildWithNameLabel(type, label, "Type Name", attrs);
    }

    private static MasterDataTypeDescriptor buildWithNameLabel(
            String type, String label, String nameLabel, MasterDataFieldDef... attrs) {
        List<MasterDataFieldDef> fields = new ArrayList<>();
        fields.add(baseCode());
        fields.add(baseName(nameLabel));
        fields.add(baseActive());
        for (MasterDataFieldDef attr : attrs) {
            fields.add(attr);
        }
        return MasterDataTypeDescriptor.builder()
                .type(type)
                .label(label)
                .fields(List.copyOf(fields))
                .build();
    }

    /** Catalog entry for nested masters (dedicated routes; no flat handler fields). */
    private static MasterDataTypeDescriptor buildMotorStages() {
        List<MasterDataFieldDef> fields = new ArrayList<>();
        fields.add(baseCode());
        fields.add(MasterDataFieldDef.builder()
                .key("name")
                .label("Name")
                .dataType("STRING")
                .required(false)
                .maxLength(255)
                .readOnlyOnUpdate(true)
                .serverGenerated(true)
                .attribute(false)
                .build());
        fields.add(baseActive());
        fields.add(attrString("projectId", "Project", 128, null, true));
        fields.add(attrInt("motorStage", "Motor Stage", 0, 99, true));
        return MasterDataTypeDescriptor.builder()
                .type(MOTOR_STAGES)
                .label("Motor Stage Master")
                .fields(List.copyOf(fields))
                .build();
    }

    private static MasterDataTypeDescriptor buildNested(String type, String label) {
        return MasterDataTypeDescriptor.builder()
                .type(type)
                .label(label)
                .fields(List.of())
                .build();
    }

    private static MasterDataFieldDef baseCode() {
        return MasterDataFieldDef.builder()
                .key("code")
                .label("Code")
                .dataType("STRING")
                .required(false)
                .maxLength(64)
                .pattern(CODE_PATTERN)
                .readOnlyOnUpdate(true)
                .serverGenerated(true)
                .attribute(false)
                .build();
    }

    /** Build PREFIX-{id} business codes for flat masters. */
    public static String generatedCode(String prefix, long id) {
        return prefix + "-" + id;
    }

    private static MasterDataFieldDef baseName(String label) {
        return MasterDataFieldDef.builder()
                .key("name")
                .label(label)
                .dataType("STRING")
                .required(true)
                .maxLength(255)
                .pattern(MASTER_DATA_NAME_PATTERN)
                .readOnlyOnUpdate(false)
                .attribute(false)
                .build();
    }

    private static MasterDataFieldDef baseActive() {
        return MasterDataFieldDef.builder()
                .key("isActive")
                .label("Active")
                .dataType("BOOLEAN")
                .required(true)
                .readOnlyOnUpdate(false)
                .attribute(false)
                .build();
    }

    private static MasterDataFieldDef attrString(String key, String label, int maxLen, String pattern) {
        return attrString(key, label, maxLen, pattern, false);
    }

    private static MasterDataFieldDef attrString(
            String key, String label, int maxLen, String pattern, boolean readOnlyOnUpdate) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("STRING")
                .required(true)
                .maxLength(maxLen)
                .pattern(pattern)
                .attribute(true)
                .readOnlyOnUpdate(readOnlyOnUpdate)
                .build();
    }

    private static MasterDataFieldDef attrOptionalString(String key, String label, int maxLen) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("STRING")
                .required(false)
                .maxLength(maxLen)
                .attribute(true)
                .readOnlyOnUpdate(false)
                .build();
    }
    private static MasterDataFieldDef attrOptionalString(
            String key, String label, int maxLen, String pattern) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("STRING")
                .required(false)
                .maxLength(maxLen)
                .pattern(pattern)
                .attribute(true)
                .readOnlyOnUpdate(false)
                .build();
    }

    private static MasterDataFieldDef attrInt(String key, String label, int min, int max) {
        return attrInt(key, label, min, max, false);
    }

    private static MasterDataFieldDef attrInt(String key, String label, int min, int max, boolean readOnlyOnUpdate) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("INTEGER")
                .required(true)
                .min(min)
                .max(max)
                .attribute(true)
                .readOnlyOnUpdate(readOnlyOnUpdate)
                .build();
    }

    private static MasterDataFieldDef attrNumber(String key, String label) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("NUMBER")
                .required(true)
                .attribute(true)
                .readOnlyOnUpdate(false)
                .build();
    }

    private static MasterDataFieldDef attrOptionalNumber(String key, String label) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("NUMBER")
                .required(false)
                .attribute(true)
                .readOnlyOnUpdate(false)
                .build();
    }

    private static MasterDataFieldDef attrBoolean(String key, String label) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("BOOLEAN")
                .required(true)
                .attribute(true)
                .readOnlyOnUpdate(false)
                .build();
    }

    private static MasterDataFieldDef attrOptionalInt(String key, String label, int min, int max) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("INTEGER")
                .required(false)
                .min(min)
                .max(max)
                .attribute(true)
                .readOnlyOnUpdate(false)
                .build();
    }

    private static MasterDataFieldDef attrServerGeneratedString(
            String key, String label, int maxLen, String pattern) {
        return MasterDataFieldDef.builder()
                .key(key)
                .label(label)
                .dataType("STRING")
                .required(false)
                .maxLength(maxLen)
                .pattern(pattern)
                .attribute(true)
                .readOnlyOnUpdate(true)
                .serverGenerated(true)
                .build();
    }
}
