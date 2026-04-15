# Photo Album Organizer Spec

**Date:** 2026-04-16  
**Owner:** Workflow: doc-to-pr-pipeline

## Problem Statement
Users have large collections of photos but no easy way to organize them into date-based albums with a visual, drag-and-drop interface. Existing tools either require manual folder management or lock users into opaque cloud platforms without intuitive reorganization.

## Goals
1. Allow users to organize photos into flat (non-nested) albums grouped by date.
2. Provide drag-and-drop reordering of albums on the main page.
3. Display photos within each album as a responsive tile/grid preview.

## Non-goals
- Nested/hierarchical album structures.
- Photo editing, filters, or transformations.
- Cloud storage or sync — photos are referenced locally or via URL.
- User authentication or multi-user collaboration (single-user app).
- Video or non-image media support.

## Functional Requirements
1. **Data model**:
   - `Album`: `id`, `title`, `date` (date the album represents), `coverPhotoId` (optional), `sortOrder` (integer for drag-and-drop position), `createdAt`, `updatedAt`.
   - `Photo`: `id`, `albumId` (FK → Album), `src` (file path or URL), `thumbnail` (optional generated thumbnail), `caption` (optional text), `sortOrder` (position within album), `createdAt`.
   - Albums are flat — no `parentAlbumId`. One photo belongs to exactly one album.
2. **Main page (Album list)**:
   - Display all albums as cards, sorted by `sortOrder`.
   - Each card shows: album title, date, cover photo thumbnail, photo count.
   - Drag-and-drop to reorder albums; new order persists immediately.
   - Button to create a new album (title + date picker).
   - Click an album card to navigate to the album detail view.
3. **Album detail page**:
   - Header: album title, date, edit/delete actions.
   - Photo grid: responsive tile layout showing photo thumbnails.
   - Add photos: file picker or drag-and-drop upload zone.
   - Click a photo tile to view full-size in a lightbox/modal.
   - Optional: drag-and-drop to reorder photos within the album.
4. **Album CRUD**:
   - Create album with title and date (auto-grouped by date if desired).
   - Edit album title/date.
   - Delete album (with confirmation; removes album record and photo associations).
5. **Photo CRUD**:
   - Add photos to an album (single or batch).
   - Remove a photo from an album.
   - Update photo caption.

## Acceptance Criteria
- User can create an album with a title and date, and it appears on the main page.
- Albums on the main page can be reordered via drag-and-drop; order persists after page refresh.
- Clicking an album shows its photos in a tile grid layout.
- User can add photos to an album and see them rendered as tiles.
- User can delete an album; it is removed from the main page.
- Albums are never nested inside other albums.
- The UI is responsive and works on desktop and tablet viewports.

## Risks / Mitigations
- **Risk**: Large photo collections cause slow rendering. **Mitigation**: Use lazy loading, virtualized grid, and thumbnail generation.
- **Risk**: Drag-and-drop library compatibility issues across browsers. **Mitigation**: Use a well-maintained library (e.g., dnd-kit or react-beautiful-dnd).
- **Risk**: File uploads for large images are slow. **Mitigation**: Client-side thumbnail generation before upload; show progress indicators.

## Implementation Plan (High Level)
1. Scaffold a React + TypeScript project with TailwindCSS and shadcn/ui.
2. Set up data layer — local state or lightweight backend (e.g., JSON server or SQLite) for albums and photos.
3. Build the main page: album card grid with drag-and-drop reordering (dnd-kit).
4. Build the album detail page: responsive photo tile grid, photo upload, lightbox preview.
5. Implement album and photo CRUD operations.
6. Add responsive styling and polish UI/UX.
7. Write tests: component tests for album list, drag-and-drop, photo grid, and CRUD flows.

## Test Plan
- Unit tests for data utilities (sorting, reordering logic).
- Component tests for AlbumCard, AlbumList (drag-and-drop), PhotoGrid, PhotoUpload.
- Integration test: create album → add photos → reorder albums → verify persistence.
- Manual QA: drag-and-drop across browsers, responsive layout on different viewports.

## Open Questions
- Should albums auto-group by date from photo EXIF metadata, or is date always manually set?
- What is the maximum number of photos per album or total albums supported?
- Should the app support a persistent backend (e.g., SQLite), or is browser localStorage/IndexedDB sufficient?
