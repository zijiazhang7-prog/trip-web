package com.trip.service;

import com.trip.dto.imports.ImportPreviewResult;
import com.trip.dto.imports.ImportRequest;
import com.trip.dto.imports.ImportResult;
import java.io.Reader;

public interface ImportService {

    void validateImportFile(ImportRequest request);

    ImportPreviewResult previewImport(ImportRequest request, Reader reader);

    ImportResult runImport(ImportRequest request, Reader reader);
}
