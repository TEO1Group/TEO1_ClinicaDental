import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DoctorService } from '../service/doctor.service';
import { DoctorResponse } from '../models/doctor.model';

@Component({
  selector: 'app-listado-doctores',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './listado-doctores.component.html',
  styleUrl: './listado-doctores.component.scss'
})
export class ListadoDoctoresComponent implements OnInit {
  private readonly doctorService = inject(DoctorService);

  private readonly _doctores = signal<DoctorResponse[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly doctores = this._doctores.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  ngOnInit(): void {
    this.cargarDoctores();
  }

  cargarDoctores(): void {
    this._cargando.set(true);
    this._error.set('');

    this.doctorService.listarDoctores().subscribe({
      next: (response) => {
        this._doctores.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar los doctores.');
      }
    });
  }
}