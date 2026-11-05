import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom';
import { fetchAvailability } from '../api/roomsApi';
import {
  BOOKING_WINDOW,
  calculateNights,
  formatDisplayDate,
} from '../utils/dateUtils';
import { formatINR } from '../utils/formatUtils';
import {
  Button,
  Badge,
  Skeleton,
  EmptyState,
  ErrorBanner,
} from '../components/ui';
import './RoomDetailPage.css';

export function RoomDetailPage() {
  const { code } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const checkIn = searchParams.get('checkIn') || BOOKING_WINDOW.DEFAULT_CHECK_IN;
  const checkOut = searchParams.get('checkOut') || BOOKING_WINDOW.DEFAULT_CHECK_OUT;
  const rooms = Number(searchParams.get('rooms')) || 1;
  const adults = Number(searchParams.get('adults')) || 2;
  const children = Number(searchParams.get('children')) || 0;

  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedPhotoIndex, setSelectedPhotoIndex] = useState(0);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await fetchAvailability({
        checkIn,
        checkOut,
        rooms,
        adults,
        children,
      });
      setData(result);
    } catch (err) {
      setError(err.message || 'Unable to load estate accommodation details.');
    } finally {
      setIsLoading(false);
    }
  }, [checkIn, checkOut, rooms, adults, children]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const nights = calculateNights(checkIn, checkOut);
  const category = data?.categories?.find(
    (c) => c.code?.toLowerCase() === code?.toLowerCase()
  );

  function getCategoryPhotos(cat) {
    if (cat?.code === 'VILLA') {
      return [
        {
          src: '/images/villa-pool.webp',
          srcSet: '/images/villa-pool.webp 800w, /images/villa-pool-1600w.webp 1600w',
          alt: 'Private Plantation Pool Villa featuring secluded plunge pool and estate panorama',
        },
        {
          src: '/images/hero-estate.webp',
          srcSet: '/images/hero-estate-800w.webp 800w, /images/hero-estate.webp 1600w',
          alt: 'Mullayanagiri foothills and estate Arabica coffee plantation surrounding the villa',
        },
      ];
    }
    return [
      {
        src: '/images/room-canopy.webp',
        srcSet: '/images/room-canopy.webp 800w, /images/room-canopy-1600w.webp 1600w',
        alt: 'Coffee Estate Canopy Room with private balcony overlooking the plantation',
      },
      {
        src: '/images/hero-estate.webp',
        srcSet: '/images/hero-estate-800w.webp 800w, /images/hero-estate.webp 1600w',
        alt: 'Mullayanagiri foothills and estate Arabica coffee plantation surrounding the cottage',
      },
    ];
  }

  const photoList = getCategoryPhotos(category);

  const backUrl = `/?checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;

  function handleSelectRatePlan(plan) {
    const checkoutUrl = `/checkout?category=${encodeURIComponent(category.code)}&plan=${encodeURIComponent(plan.code)}&checkIn=${encodeURIComponent(checkIn)}&checkOut=${encodeURIComponent(checkOut)}&rooms=${rooms}&adults=${adults}&children=${children}`;
    navigate(checkoutUrl);
  }

  return (
    <div className="room-detail-page">
      {/* 1. Breadcrumb Navigation */}
      <nav className="room-detail__nav" aria-label="Breadcrumb">
        <Link to={backUrl} className="room-detail__back-link">
          &larr; Back to available accommodations
        </Link>
      </nav>

      {/* Loading Skeletons */}
      {isLoading && (
        <div className="room-detail__layout" aria-label="Loading room details" aria-busy="true">
          <div className="room-gallery">
            <Skeleton variant="rectangular" className="room-gallery__main-wrap" />
            <div className="room-gallery__thumbnails">
              <Skeleton variant="rectangular" width="5.5rem" height="3.75rem" />
              <Skeleton variant="rectangular" width="5.5rem" height="3.75rem" />
            </div>
          </div>
          <div className="room-info">
            <Skeleton variant="text" width="60%" height="2rem" />
            <Skeleton variant="text" width="40%" height="1.25rem" />
            <Skeleton variant="rectangular" width="100%" height="140px" />
            <Skeleton variant="rectangular" width="100%" height="100px" />
            <Skeleton variant="rectangular" width="100%" height="100px" />
          </div>
        </div>
      )}

      {/* Error Banner */}
      {!isLoading && error && (
        <ErrorBanner
          variant="error"
          title="Could not retrieve accommodation details"
          message={error}
          actionLabel="Retry"
          onAction={loadData}
        />
      )}

      {/* Not Found / Empty State */}
      {!isLoading && !error && !category && (
        <EmptyState
          title={`Room Category "${code}" Not Found`}
          description="We could not locate this accommodation category. Please review all available estate rooms and villas."
          action={
            <Button variant="primary" onClick={() => navigate(backUrl)}>
              Browse available rooms
            </Button>
          }
        />
      )}

      {/* Main Room Showcase */}
      {!isLoading && !error && category && (
        <>
          <header className="room-detail__header">
            <div className="room-detail__meta-row">
              <Badge variant="neutral">{category.code}</Badge>
              {category.isBlackedOut ? (
                <Badge variant="danger">
                  Blackout: {category.blackoutReason || 'Reserved for event'}
                </Badge>
              ) : category.remainingAllotment <= 3 && category.remainingAllotment > 0 ? (
                <Badge variant="warning">
                  Only {category.remainingAllotment} left on website
                </Badge>
              ) : (
                <Badge variant="success">
                  {category.remainingAllotment} units available online
                </Badge>
              )}
            </div>

            <h1 className="room-detail__title">{category.name}</h1>

            <div className="room-detail__stay-strip">
              <span className="room-detail__stay-item">
                {formatDisplayDate(checkIn)} &ndash; {formatDisplayDate(checkOut)}
              </span>
              <span className="room-detail__stay-divider" aria-hidden="true">&bull;</span>
              <span className="room-detail__stay-item">
                {nights} {nights === 1 ? 'Night' : 'Nights'}
              </span>
              <span className="room-detail__stay-divider" aria-hidden="true">&bull;</span>
              <span className="room-detail__stay-item">
                {adults} {adults === 1 ? 'Adult' : 'Adults'}{children > 0 ? `, ${children} Children` : ''} &bull; {rooms} {rooms === 1 ? 'Room' : 'Rooms'}
              </span>
            </div>
          </header>

          <div className="room-detail__layout">
            {/* Gallery Left */}
            <div className="room-gallery">
              <div className="room-gallery__main-wrap">
                <img
                  src={photoList[selectedPhotoIndex]?.src || photoList[0].src}
                  srcSet={photoList[selectedPhotoIndex]?.srcSet || photoList[0].srcSet}
                  sizes="(max-width: 1024px) 100vw, 700px"
                  alt={photoList[selectedPhotoIndex]?.alt || photoList[0].alt}
                  width="800"
                  height="500"
                  loading="eager"
                  className="room-gallery__main-img"
                />
              </div>

              {photoList.length > 1 && (
                <div className="room-gallery__thumbnails" role="tablist" aria-label="Room photo gallery">
                  {photoList.map((photo, idx) => (
                    <button
                      key={idx}
                      type="button"
                      role="tab"
                      aria-selected={selectedPhotoIndex === idx}
                      aria-label={`View photo ${idx + 1}`}
                      className={`room-gallery__thumb-btn ${selectedPhotoIndex === idx ? 'room-gallery__thumb-btn--active' : ''}`.trim()}
                      onClick={() => setSelectedPhotoIndex(idx)}
                    >
                      <img
                        src={photo.src}
                        alt=""
                        width="88"
                        height="60"
                        loading="lazy"
                        className="room-gallery__thumb-img"
                      />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Information & Rate Plans Right */}
            <div className="room-info">
              <div className="room-info__description">
                <h2 className="room-info__heading">About this Sanctuary</h2>
                <p className="room-info__body">{category.baseDescription}</p>

                <div className="room-info__amenities">
                  <div className="room-info__amenity-item">
                    <span className="room-info__bullet" aria-hidden="true">&bull;</span>
                    <span>Max {category.maxAdults} Adults, {category.maxChildren} Children</span>
                  </div>
                  <div className="room-info__amenity-item">
                    <span className="room-info__bullet" aria-hidden="true">&bull;</span>
                    <span>{category.totalPhysicalKeys} Total Estate Keys</span>
                  </div>
                  <div className="room-info__amenity-item">
                    <span className="room-info__bullet" aria-hidden="true">&bull;</span>
                    <span>Artisanal French Press &amp; Estate Roast</span>
                  </div>
                  <div className="room-info__amenity-item">
                    <span className="room-info__bullet" aria-hidden="true">&bull;</span>
                    <span>Organic Botanical Bath Amenities</span>
                  </div>
                </div>
              </div>

              {category.isBlackedOut && (
                <ErrorBanner
                  variant="warning"
                  title="Dates Currently Blacked Out"
                  message={category.blackoutReason || 'This room category is reserved for an exclusive private event on your selected dates.'}
                  actionLabel="Change dates"
                  onAction={() => navigate(backUrl)}
                />
              )}

              {/* Rate Plans List */}
              <section className="rate-plans" aria-labelledby="rate-plans-heading">
                <h2 id="rate-plans-heading" className="rate-plans__title">
                  Select Rate Plan &amp; Dining
                </h2>

                {category.ratePlans && category.ratePlans.length > 0 ? (
                  category.ratePlans.map((plan) => (
                    <div key={plan.id || plan.code} className="rate-plan-card">
                      <div className="rate-plan-card__info">
                        <div className="rate-plan-card__title-row">
                          <h3 className="rate-plan-card__name">{plan.name}</h3>
                          <Badge variant="neutral">{plan.code}</Badge>
                        </div>
                        <p className="rate-plan-card__desc">
                          {plan.code === 'EP' && 'Accommodation only. Savor dining across our plantation restaurants and in-room service.'}
                          {plan.code === 'CP' && 'Includes daily artisanal plantation breakfast buffet for all guests at the Arabica Pavilion.'}
                          {plan.code === 'MAP' && 'Includes daily breakfast and signature multi-course dinner overlooking the valley.'}
                        </p>
                      </div>

                      <div className="rate-plan-card__action-group">
                        <span className="rate-plan-card__price">
                          {formatINR(plan.totalPayableInr)}
                        </span>
                        <span className="rate-plan-card__tax-note">
                          Taxes shown are indicative in this test build
                        </span>
                        <Button
                          variant="primary"
                          disabled={!category.hasAllotment || category.isBlackedOut}
                          onClick={() => handleSelectRatePlan(plan)}
                        >
                          Select
                        </Button>
                      </div>
                    </div>
                  ))
                ) : (
                  <p style={{ color: 'var(--color-text-muted)' }}>
                    No rate plans available for this category on the chosen dates.
                  </p>
                )}
              </section>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export default RoomDetailPage;
