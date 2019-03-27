package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.certificate.attributes.CertificateIdentifier;
import hirs.data.persist.certificate.attributes.ComponentAddress;
import hirs.data.persist.certificate.attributes.ComponentClass;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import hirs.data.persist.certificate.attributes.URIReference;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Basic class that handle component identifiers from the Platform Configuration
 * Attribute.
 * <pre>
 * ComponentIdentifier ::= SEQUENCE {
 *      componentManufacturer UTF8String (SIZE (1..STRMAX)),
 *      componentModel UTF8String (SIZE (1..STRMAX)),
 *      componentSerial[0] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentRevision [1] IMPLICIT UTF8String (SIZE (1..STRMAX)) OPTIONAL,
 *      componentManufacturerId [2] IMPLICIT PrivateEnterpriseNumber OPTIONAL,
 *      fieldReplaceable [3] IMPLICIT BOOLEAN OPTIONAL,
 *      componentAddress [4] IMPLICIT
 *          SEQUENCE(SIZE(1..CONFIGMAX)) OF ComponentAddress OPTIONAL
 *      componentPlatformCert [5] IMPLICIT CertificateIdentifier OPTIONAL,
 *      componentPlatformCertUri [6] IMPLICIT URIReference OPTIONAL,
 *      status [7] IMPLICIT AttributeStatus OPTIONAL }
 * where STRMAX is 256, CONFIGMAX is 32
 * </pre>
 */
public class ComponentIdentifierV2 extends ComponentIdentifier {
    private static final Logger LOGGER = LogManager.getLogger(ComponentIdentifierV2.class);

    private static final String COMPONENT_CLASS = "2.23.133.18.3.1";
    private static final int MANDATORY_ELEMENTS = 3;

    private static final int COMPONENT_PLATFORM_CERT = 5;
    private static final int COMPONENT_PLATFORM_URI = 6;
    private static final int ATTRIBUTE_STATUS = 7;

    private String componentClass;
    private CertificateIdentifier certificateIdentifier;
    private URIReference componentPlatformUri;
    private AttributeStatus attributeStatus;

    /**
     * A type to handle the security Level used in the FIPS Level.
     * Ordering of enum types is intentional and their ordinal values correspond to enum
     * values in the TCG spec.
     *
     * <pre>
     * AttributeStatus ::= ENUMERATED {
     *      added (0),
     *      modified (1),
     *      removed (2)}
     * </pre>
     */
    public enum AttributeStatus {
        /**
         * Attribute Status for ADDED.
         */
        ADDED("added"),
        /**
         * Attribute Status for MODIFIED.
         */
        MODIFIED("modified"),
        /**
         * Attribute Status for REMOVED.
         */
        REMOVED("removed"),
        /**
         * Attribute Status for NOT_SPECIFIED.
         */
        NOT_SPECIFIED(EMPTY_COMPONENT);

        private final String value;

        /**
         * Basic constructor.
         * @param value string containing the value.
         */
        AttributeStatus(final String value) {
            this.value = value;
        }

        /**
         * Getter for the string of attribute status value.
         * @return the string containing the value.
         */
        public String getValue() {
            return this.value;
        }
    }

    /**
     * Default constructor.
     */
    public ComponentIdentifierV2() {
        super();
        componentClass = EMPTY_COMPONENT;
        certificateIdentifier = null;
        componentPlatformUri = null;
        attributeStatus = AttributeStatus.NOT_SPECIFIED;
    }

    /**
     * Constructor given the components values.
     *
     * @param componentClass represent the component type
     * @param componentManufacturer represents the component manufacturer
     * @param componentModel represents the component model
     * @param componentSerial  represents the component serial number
     * @param componentRevision represents the component revision
     * @param componentManufacturerId represents the component manufacturer ID
     * @param fieldReplaceable represents if the component is replaceable
     * @param componentAddress represents a list of addresses
     * @param certificateIdentifier object representing certificate Id
     * @param componentPlatformUri object containing the URI Reference
     * @param attributeStatus object containing enumerated status
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public ComponentIdentifierV2(final String componentClass,
            final DERUTF8String componentManufacturer,
            final DERUTF8String componentModel,
            final DERUTF8String componentSerial,
            final DERUTF8String componentRevision,
            final ASN1ObjectIdentifier componentManufacturerId,
            final ASN1Boolean fieldReplaceable,
            final List<ComponentAddress> componentAddress,
            final CertificateIdentifier certificateIdentifier,
            final URIReference componentPlatformUri,
            final AttributeStatus attributeStatus) {
        super(componentManufacturer, componentModel, componentSerial,
                componentRevision, componentManufacturerId, fieldReplaceable,
                componentAddress);

        this.componentClass = componentClass;
        this.certificateIdentifier = certificateIdentifier;
        this.componentPlatformUri = componentPlatformUri;
        this.attributeStatus = attributeStatus;
    }

    /**
     * Constructor given the SEQUENCE that contains Component Identifier.
     * @param sequence containing the the component identifier
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public ComponentIdentifierV2(final ASN1Sequence sequence)
            throws IllegalArgumentException {
        // set all optional values to default in case they aren't set.
        super();
        //Check if it have a valid number of identifiers
        if (sequence.size() < MANDATORY_ELEMENTS) {
            throw new IllegalArgumentException("Component identifier do not have required values.");
        }

        int tag = 0;
        ASN1Sequence componentIdSeq = ASN1Sequence.getInstance(sequence.getObjectAt(tag++));
        componentClass = new ComponentClass(DEROctetString.getInstance(componentIdSeq
                .getObjectAt(tag)).toString()).toString();

        //Mandatory values
        this.setComponentManufacturer(DERUTF8String.getInstance(sequence.getObjectAt(tag++)));
        this.setComponentModel(DERUTF8String.getInstance(sequence.getObjectAt(tag++)));

        //Continue reading the sequence if it does contain more than 2 values
        for (int i = tag; i < sequence.size(); i++) {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));
            switch (taggedObj.getTagNo()) {
                case COMPONENT_SERIAL:
                    this.setComponentSerial(DERUTF8String.getInstance(taggedObj, false));
                    break;
                case COMPONENT_REVISION:
                    this.setComponentRevision(DERUTF8String.getInstance(taggedObj, false));
                    break;
                case COMPONENT_MANUFACTURER_ID:
                    this.setComponentManufacturerId(ASN1ObjectIdentifier
                            .getInstance(taggedObj, false));
                    break;
                case FIELD_REPLACEABLE:
                    this.setFieldReplaceable(ASN1Boolean.getInstance(taggedObj, false));
                    break;
                case COMPONENT_ADDRESS:
                    ASN1Sequence addressesSequence = ASN1Sequence.getInstance(taggedObj, false);
                    this.setComponentAddress(retrieveComponentAddress(addressesSequence));
                    break;
                case COMPONENT_PLATFORM_CERT:
                    ASN1Sequence ciSequence = ASN1Sequence.getInstance(taggedObj, false);
                    certificateIdentifier = new CertificateIdentifier(ciSequence);
                    break;
                case COMPONENT_PLATFORM_URI:
                    ASN1Sequence uriSequence = ASN1Sequence.getInstance(taggedObj, false);
                    this.componentPlatformUri = new URIReference(uriSequence);
                    break;
                case ATTRIBUTE_STATUS:
                    ASN1Enumerated enumerated = ASN1Enumerated.getInstance(taggedObj, false);
                    this.attributeStatus = AttributeStatus.values()[
                            enumerated.getValue().intValue()];
                    break;
                default:
                    throw new IllegalArgumentException("Component identifier contains "
                            + "invalid tagged object.");
            }
        }
    }

    /**
     * @return string for the type of component.
     */
    public String getComponentClass() {
        return componentClass;
    }

    /**
     * @param componentClass the type of component to set
     */
    public void setComponentClass(final String componentClass) {
        this.componentClass = componentClass;
    }

    /**
     * @return string for the certificate identifier
     */
    public CertificateIdentifier getCertificateIdentifier() {
        return certificateIdentifier;
    }

    /**
     * @param certificateIdentifier the certificate id to set
     */
    public void setCertificateIdentifier(final CertificateIdentifier certificateIdentifier) {
        this.certificateIdentifier = certificateIdentifier;
    }

    /**
     * @return the componentPlatformUri.
     */
    public URIReference getComponentPlatformUri() {
        return componentPlatformUri;
    }

    /**
     * @param componentPlatformUri the componentPlatformUri to set.
     */
    public void setComponentPlatformUri(final URIReference componentPlatformUri) {
        this.componentPlatformUri = componentPlatformUri;
    }

    /**
     * @return the attribute enumerated status.
     */
    public AttributeStatus getAttributeStatus() {
        return attributeStatus;
    }

    /**
     * @param attributeStatus the attributeState to set.
     */
    public void setAttributeStatus(final AttributeStatus attributeStatus) {
        this.attributeStatus = attributeStatus;
    }

    /**
     * @return true if the component has been modified.
     */
    public final boolean isModified() {
        return getAttributeStatus() == AttributeStatus.MODIFIED;
    }

    /**
     * @return true if the component has been removed.
     */
    public final boolean isRemoved() {
        return getAttributeStatus() != AttributeStatus.REMOVED;
    }

    /**
     * @return indicates the type of platform certificate.
     */
    @Override
    public boolean isVersion2() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentIdentifier{");
        sb.append("componentClass=").append(componentClass);
        sb.append(", componentManufacturer=").append(getComponentManufacturer()
                .getString());
        sb.append(", componentModel=").append(getComponentModel().getString());
        //Optional not null values
        sb.append(", componentSerial=");
        if (getComponentSerial() != null) {
            sb.append(getComponentSerial().getString());
        }
        sb.append(", componentRevision=");
        if (getComponentRevision() != null) {
            sb.append(getComponentRevision().getString());
        }
        sb.append(", componentManufacturerId=");
        if (getComponentManufacturerId() != null) {
            sb.append(getComponentManufacturerId().getId());
        }
        sb.append(", fieldReplaceable=");
        if (getFieldReplaceable() != null) {
            sb.append(getFieldReplaceable().toString());
        }
        sb.append(", componentAddress=");
        if (getComponentAddress().size() > 0) {
            sb.append(getComponentAddress()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));
        }
        sb.append(", certificateIdentifier=");
        if (certificateIdentifier != null) {
            sb.append(certificateIdentifier.toString());
        }
        sb.append(", componentPlatformUri=");
        if (componentPlatformUri != null) {
            sb.append(componentPlatformUri.toString());
        }
        sb.append(", status=");
        if (attributeStatus != null) {
            sb.append(attributeStatus.getValue());
        }
        sb.append("}");

        return sb.toString();
    }
}
