import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BeneficioService } from './beneficio.service';
import { Beneficio, BeneficioRequest, TransferRequest } from '../models/beneficio.model';

describe('BeneficioService', () => {
  let service: BeneficioService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/v1/beneficios';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BeneficioService]
    });
    service = TestBed.inject(BeneficioService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('deve ser criado', () => {
    expect(service).toBeTruthy();
  });

  it('getAll() deve fazer GET e retornar lista de benefícios', () => {
    const mock: Beneficio[] = [
      { id: 1, nome: 'A', valor: 1000, ativo: true, version: 0 },
      { id: 2, nome: 'B', valor: 500,  ativo: true, version: 0 }
    ];

    service.getAll().subscribe(data => {
      expect(data.length).toBe(2);
      expect(data[0].nome).toBe('A');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('GET');
    req.flush(mock);
  });

  it('create() deve fazer POST com o dto correto', () => {
    const dto: BeneficioRequest = { nome: 'Novo', valor: 200, ativo: true };
    const response: Beneficio = { id: 3, nome: 'Novo', valor: 200, ativo: true, version: 0 };

    service.create(dto).subscribe(b => expect(b.id).toBe(3));

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush(response);
  });

  it('delete() deve fazer DELETE no endpoint correto', () => {
    service.delete(1).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('transfer() deve fazer POST em /transfer com dto', () => {
    const dto: TransferRequest = { fromId: 1, toId: 2, amount: 300 };

    service.transfer(dto).subscribe();

    const req = httpMock.expectOne(`${baseUrl}/transfer`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(dto);
    req.flush(null);
  });
});
