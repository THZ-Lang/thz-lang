package thz.lang.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import thz.lang.api.dto.AnaliseRequest;
import thz.lang.api.dto.AnaliseResponse;
import thz.lang.api.dto.AstResponse;
import thz.lang.api.dto.AuditoriaResponse;
import thz.lang.api.dto.DocumentacaoResponse;
import thz.lang.api.dto.ExecucaoRequest;
import thz.lang.api.dto.ExecucaoResponse;
import thz.lang.api.dto.FormatacaoRequest;
import thz.lang.api.dto.FormatacaoResponse;
import thz.lang.api.dto.HealthResponse;
import thz.lang.api.dto.HoverRequest;
import thz.lang.api.dto.HoverResponse;
import thz.lang.api.dto.IrResponse;
import thz.lang.api.dto.SimdResponse;
import thz.lang.api.service.ThzService;

@RestController
@RequestMapping("/api")
public class ThzController {

    private final ThzService thzService;

    public ThzController(ThzService thzService) {
        this.thzService = thzService;
    }

    // ---- Análise ----

    @PostMapping("/analyze")
    public ResponseEntity<AnaliseResponse> analisar(@Valid @RequestBody AnaliseRequest request) {
        ThzService.AnaliseResult r = thzService.analisar(request.fonte(), request.estrito());
        return ResponseEntity.ok(new AnaliseResponse(
                r.diagnosticos(), r.textoDiagnosticos(), r.temErros(), r.simbolos(), r.astJson()));
    }

    @PostMapping("/hover")
    public ResponseEntity<HoverResponse> hover(@Valid @RequestBody HoverRequest request) {
        ThzService.HoverResult r = thzService.obterHover(request.fonte(), request.linha(), request.coluna());
        if (r == null)
            return ResponseEntity.ok(null);
        return ResponseEntity.ok(new HoverResponse(r.conteudo(), r.range()));
    }

    // ---- AST ----

    @PostMapping("/ast")
    public ResponseEntity<AstResponse> ast(@Valid @RequestBody AnaliseRequest request) {
        ThzService.AstResult r = thzService.obterAst(request.fonte());
        return ResponseEntity.ok(new AstResponse(r.astJson(), r.nomePrograma()));
    }

    // ---- Formatação ----

    @PostMapping("/format")
    public ResponseEntity<FormatacaoResponse> formatar(@Valid @RequestBody FormatacaoRequest request) {
        ThzService.FormatacaoResult r = thzService.formatar(request.fonte());
        return ResponseEntity.ok(new FormatacaoResponse(r.resultado(), r.alterou()));
    }

    // ---- Documentação ----

    @PostMapping("/doc")
    public ResponseEntity<DocumentacaoResponse> documentar(@Valid @RequestBody AnaliseRequest request) {
        ThzService.DocumentacaoResult r = thzService.gerarDocumentacao(request.fonte());
        return ResponseEntity.ok(new DocumentacaoResponse(r.markdown()));
    }

    // ---- Auditoria de Governança ----

    @PostMapping("/audit")
    public ResponseEntity<AuditoriaResponse> auditar(@Valid @RequestBody AnaliseRequest request) {
        ThzService.AuditoriaResult r = thzService.auditar(request.fonte());
        return ResponseEntity.ok(new AuditoriaResponse(r.relatorioJson(), r.markdown()));
    }

    // ---- IR ----

    @PostMapping("/ir")
    public ResponseEntity<IrResponse> gerarIr(@Valid @RequestBody AnaliseRequest request) {
        ThzService.IrResult r = thzService.gerarIr(request.fonte());
        return ResponseEntity.ok(new IrResponse(r.irJson(), r.llvm()));
    }

    // ---- SIMD ----

    @PostMapping("/simd")
    public ResponseEntity<SimdResponse> validarSimd(@Valid @RequestBody AnaliseRequest request) {
        ThzService.SimdResult r = thzService.validarSimd(request.fonte());
        return ResponseEntity.ok(new SimdResponse(r.resultados()));
    }

    // ---- Execução ----

    @PostMapping("/run")
    public ResponseEntity<ExecucaoResponse> executar(@Valid @RequestBody ExecucaoRequest request) {
        ThzService.ExecucaoResult r = thzService.executar(request.fonte(), request.operacao());
        return ResponseEntity.ok(new ExecucaoResponse(r.saida(), r.erros(), r.resultado()));
    }

    // ---- Health ----

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(
                "UP",
                "2.3.3",
                System.getProperty("java.version"),
                "thz-core-jvm"));
    }
}
