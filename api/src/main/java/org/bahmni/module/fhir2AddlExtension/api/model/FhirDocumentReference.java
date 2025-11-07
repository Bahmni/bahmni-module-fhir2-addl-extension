package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.BaseCustomizableData;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class FhirDocumentReference extends BaseCustomizableData<FhirDocumentReferenceAttribute> {
    @EqualsAndHashCode.Include
    private Integer documentReferenceId;

    private Patient subject;

    private Order order;

    private FhirDocumentReferenceStatus status;

    private FhirDocumentReferenceDocStatus docStatus;

    private String masterIdentifier;

    private String description;

    private Date dateStarted;

    private Date dateEnded;

    private Concept docType;

    private Location location;

    private Encounter encounter;

    private Provider provider;

    private Concept securityLabel;

    private Set<FhirDocumentReferenceContent> contents = new HashSet<>();

    public void addContent(FhirDocumentReferenceContent documentReferenceContent) {
        documentReferenceContent.setDocumentReference(this);
        contents.add(documentReferenceContent);
    }

    @Override
    public Integer getId() {
        return getDocumentReferenceId();
    }

    @Override
    public void setId(Integer id) {
        setDocumentReferenceId(id);
    }


    public enum FhirDocumentReferenceStatus {
        CURRENT,
        SUPERSEDED,
        ENTEREDINERROR
    }

    public enum FhirDocumentReferenceDocStatus {
        PRELIMINARY,
        FINAL,
        AMENDED,
        ENTEREDINERROR
    }

}
