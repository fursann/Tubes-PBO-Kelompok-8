# Tugas Besar PBO — CleanHub

**Kelas IF-48-04 · Kelompok 8**

| | |
|---|---|
| **Repository** | `Tugas-Besar-IF-48-04-Kelompok_8` |
| **Topik** | Sistem Manajemen Laundry berbasis OOP (PBO) |
| **Stack** | Java 17, MySQL, HTML/CSS/JS, REST API |

## Deskripsi

**CleanHub** — aplikasi manajemen laundry dengan konsep Pemrograman Berorientasi Objek.

Fitur utama: login multi-role (Customer, Staff, Admin), registrasi user, buat & lacak pesanan, update status laundry, dashboard admin, integrasi MySQL, antarmuka web + konsol.

## Anggota Kelompok 8

| No | Nama | NIM | Kontribusi |
|----|------|-----|------------|
| 1  | *(isi)* | *(isi)* | *(isi)* |
| 2  | *(isi)* | *(isi)* | *(isi)* |
| 3  | *(isi)* | *(isi)* | *(isi)* |
| 4  | *(isi)* | *(isi)* | *(isi)* |

## Struktur Project

```
Tugas-Besar-IF-48-04-Kelompok_8/
├── README.md
├── .gitignore
├── database/cleanhub.sql
├── docs/
│   ├── TUTORIAL_GITHUB.md      ← panduan GitHub lengkap
│   ├── LAPORAN_ALUR_DETAIL.md
│   └── LAPORAN_ALUR_RINGKAS.md
├── web/                        ← frontend
└── TubesTest/                  ← backend Java + run.bat
```

## Cara Menjalankan

```powershell
# Import database/cleanhub.sql di phpMyAdmin (XAMPP MySQL ON)

copy TubesTest\src\main\resources\database.properties.example TubesTest\src\main\resources\database.properties

cd TubesTest
.\run.bat
```

Browser: **http://localhost:8080/**

Akun demo: `C-001` · `S-001` · `A-001`

## Push ke GitHub

Ganti `USERNAME` dengan username GitHub Anda:

```powershell
cd "d:\apps\folder tugas\PBO\tubes pbo"

git remote set-url origin https://github.com/USERNAME/Tugas-Besar-IF-48-04-Kelompok_8.git
git push -u origin main
```

## Clone (anggota lain)

```powershell
git clone https://github.com/USERNAME/Tugas-Besar-IF-48-04-Kelompok_8.git
```

## Alur Kerja Git

```powershell
git checkout -b feature_nama_fitur
git add .
git commit -m "Menambahkan fitur X"
git push origin feature_nama_fitur
```

Merge via **Pull Request** di GitHub.

Panduan lengkap: [`docs/TUTORIAL_GITHUB.md`](docs/TUTORIAL_GITHUB.md)
